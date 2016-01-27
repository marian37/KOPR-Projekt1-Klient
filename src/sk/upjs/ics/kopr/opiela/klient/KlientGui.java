package sk.upjs.ics.kopr.opiela.klient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;

import net.miginfocom.swing.MigLayout;

public class KlientGui {

	private static final int MIN_SOCKETS = 1;

	private static final int MAX_SOCKETS = 20;

	private static final String ADRESAR_PRE_ULOZENIE = "Stiahnuté/";

	private JFrame frame = new JFrame("Klient");

	private JLabel lblAdresaServeru = new JLabel("Adresa serveru:");

	private JTextField txtAdresaServeru = new JTextField("localhost");

	private JLabel lblPort = new JLabel("Port:");

	private JTextField txtPort = new JTextField("5000");

	private JLabel lblPocetSoketov = new JLabel("Počet soketov:");

	private JSpinner pocetSoketovSpinner = new JSpinner(new SpinnerNumberModel(
			1, MIN_SOCKETS, MAX_SOCKETS, 1));

	private JButton btnPotvrdit = new JButton("Potvrdiť");

	private JLabel lblNazovSuboru = new JLabel();

	private JProgressBar progressBar = new JProgressBar();

	private JButton btnStartPause = new JButton("Spustiť sťahovanie");

	private JButton btnStop = new JButton("Ukončiť sťahovanie");

	private InetAddress adresa;

	private int cisloPortu;

	private int pocetSoketov;

	private File subor;

	private long velkostSuboru;

	private int velkostChunku;

	private boolean pokracovat = false;

	public KlientGui() {
		frame.setLayout(new MigLayout("wrap 2", "[][grow, fill]",
				"[][][][nogrid][][][nogrid]"));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.add(lblAdresaServeru);
		frame.add(txtAdresaServeru);
		frame.add(lblPort);
		frame.add(txtPort);
		frame.add(lblPocetSoketov);
		frame.add(pocetSoketovSpinner);
		frame.add(btnPotvrdit, "wrap");
		frame.add(lblNazovSuboru, "span 2");
		frame.add(progressBar, "span 2");
		frame.add(btnStartPause, "tag ok");
		frame.add(btnStop, "tag cancel");

		lblNazovSuboru.setVisible(false);
		progressBar.setVisible(false);
		btnStartPause.setEnabled(false);
		btnStop.setEnabled(false);

		btnPotvrdit.setMnemonic('a');
		btnStartPause.setMnemonic('s');
		btnStop.setMnemonic('d');

		btnStop.setToolTipText("Ukončí sťahovanie bez možnosti pokračovať v ňom.");

		btnPotvrdit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				btnPotvrditActionPerformed();
			}
		});

		btnStartPause.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				btnStartPauseActionPerformed();
			}
		});

		btnStop.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				btnStopActionPerformed();
			}
		});

		frame.pack();
		frame.setVisible(true);
	}

	private void btnPotvrditActionPerformed() {
		try {
			String adresaServeru = txtAdresaServeru.getText();
			cisloPortu = Integer.parseInt(txtPort.getText());
			pocetSoketov = (Integer) pocetSoketovSpinner.getValue();

			adresa = InetAddress.getByName(adresaServeru);

			System.out.println(adresa + " " + cisloPortu + " " + pocetSoketov);

			txtAdresaServeru.setEnabled(false);
			txtPort.setEnabled(false);
			pocetSoketovSpinner.setEnabled(false);
			btnPotvrdit.setEnabled(false);

			progressBar.setIndeterminate(true);
			progressBar.setVisible(true);

			overSpojenie();
		} catch (NumberFormatException exception) {
			JOptionPane.showMessageDialog(frame, "Port musí byť celé číslo.",
					"Varovanie", JOptionPane.WARNING_MESSAGE);
		} catch (UnknownHostException exception) {
			JOptionPane.showMessageDialog(frame, "Zlá adresa serveru.",
					"Varovanie", JOptionPane.WARNING_MESSAGE);
		}
	}

	private void overSpojenie() {
		final SwingWorker<Integer, Void> testSpojenia = new SwingWorker<Integer, Void>() {

			@Override
			protected Integer doInBackground() throws Exception {
				Socket soket = new Socket(adresa, cisloPortu);

				PrintWriter pw = new PrintWriter(soket.getOutputStream());
				InputStream is = soket.getInputStream();
				BufferedReader br = new BufferedReader(
						new InputStreamReader(is));

				pw.println("info");
				pw.flush();

				String nazovSuboru = br.readLine();
				subor = new File(ADRESAR_PRE_ULOZENIE + nazovSuboru);
				velkostSuboru = Long.parseLong(br.readLine());
				velkostChunku = Integer.parseInt(br.readLine());

				System.out.println(subor + " " + velkostSuboru + " "
						+ velkostChunku);

				// zisti aktuálny stav sťahovania
				if (!subor.isDirectory() && subor.exists()) {
					BufferedInputStream bis = new BufferedInputStream(
							new FileInputStream(subor));
					byte[] bytes = new byte[velkostChunku];
					int pocet_chunkov = (int) velkostSuboru / velkostChunku;
					Integer aktualnyStav = pocet_chunkov;
					for (int i = 0; i < pocet_chunkov; i++) {
						bis.read(bytes, i * velkostChunku, velkostChunku);
						for (int j = 0; j < bytes.length; j++) {
							if (bytes[j] != 0) {
								aktualnyStav--;
								break;
							}
						}
					}
					bis.close();
					return aktualnyStav;
				}

				soket.close();

				return null;
			}

			@Override
			protected void done() {
				Integer aktualnyStav = null;
				// kontrola, či nenastala výnimka
				try {
					aktualnyStav = get();
				} catch (InterruptedException e) {
					JOptionPane.showMessageDialog(frame,
							"Zlyhalo spojenie so serverom.", "Varovanie",
							JOptionPane.WARNING_MESSAGE);
				} catch (ExecutionException e) {
					JOptionPane.showMessageDialog(frame,
							"Zlyhalo spojenie so serverom.", "Varovanie",
							JOptionPane.WARNING_MESSAGE);
				}

				progressBar.setVisible(false);
				progressBar.setIndeterminate(false);

				if (subor != null) {
					if (!subor.isDirectory() && subor.exists()) {
						btnStartPause.setText("Pokračovať v sťahovaní");
						pokracovat = true;
						progressBar.setValue(aktualnyStav);
					}
					btnStartPause.setEnabled(true);
					btnStop.setEnabled(true);
					lblNazovSuboru.setText(ADRESAR_PRE_ULOZENIE
							+ subor.getName());
					lblNazovSuboru.setVisible(true);

					progressBar.setStringPainted(true);
					progressBar.setMinimum(0);
					progressBar.setMaximum((int) velkostSuboru / velkostChunku);
					// TODO +1 ???
				} else {
					txtAdresaServeru.setEnabled(true);
					txtPort.setEnabled(true);
					pocetSoketovSpinner.setEnabled(true);
					btnPotvrdit.setEnabled(true);
				}
			}
		};

		testSpojenia.execute();
	}

	private void btnStartPauseActionPerformed() {
		// TODO skontroluj či začína, alebo pokračuje
		// TODO spusti sťahovanie
		SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {

			@Override
			protected Void doInBackground() throws Exception {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			protected void process(List<Integer> chunks) {
				// TODO Auto-generated method stub
			}
		};
		worker.execute();
	}

	private void btnStopActionPerformed() {
		// TODO Otestuj!
		// vymaže všetko stiahnuté
		boolean uspesne = true;
		if (subor != null && !subor.isDirectory() && subor.exists()) {
			uspesne = subor.delete();
		}
		if (!uspesne) {
			JOptionPane
					.showMessageDialog(frame, "Nepodarilo sa zmazať súbor "
							+ subor.getName(), "Varovanie",
							JOptionPane.WARNING_MESSAGE);
		} else {
			frame.dispatchEvent(new WindowEvent(frame,
					WindowEvent.WINDOW_CLOSING));
		}
	}

}
