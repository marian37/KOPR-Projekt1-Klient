package sk.upjs.ics.kopr.opiela.klient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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

public class KlientGui implements PropertyChangeListener {

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
			4, MIN_SOCKETS, MAX_SOCKETS, 1));

	private JButton btnPotvrdit = new JButton("Potvrdiť");

	private JLabel lblNazovSuboru = new JLabel();

	private JProgressBar progressBar = new JProgressBar();

	private JButton btnStartPause = new JButton("Spustiť sťahovanie");

	private JButton btnStop = new JButton("Zrušiť sťahovanie");

	private InetAddress adresa;

	private int cisloPortu;

	private int pocetSoketov;

	private File subor;

	private long velkostSuboru;

	private int dlzkaChunku;

	private int pocetChunkov;

	private CopyOnWriteArrayList<Integer> aktualnyStav;

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
		final SwingWorker<Void, Void> testSpojenia = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				Socket soket = new Socket(adresa, cisloPortu);

				PrintWriter pw = new PrintWriter(soket.getOutputStream());
				InputStream is = soket.getInputStream();
				BufferedReader br = new BufferedReader(
						new InputStreamReader(is));

				pw.println("info");
				pw.flush();

				String nazovSuboru = br.readLine();
				new File(ADRESAR_PRE_ULOZENIE).mkdirs();
				subor = new File(ADRESAR_PRE_ULOZENIE + nazovSuboru);
				velkostSuboru = Long.parseLong(br.readLine());
				dlzkaChunku = Integer.parseInt(br.readLine());

				pocetChunkov = (int) velkostSuboru / dlzkaChunku;
				if (!(velkostSuboru % dlzkaChunku == 0)) {
					pocetChunkov++;
				}

				System.out.println(subor + " " + velkostSuboru + " "
						+ dlzkaChunku + " " + pocetChunkov);

				List<Integer> aktualnyStavList = new ArrayList<Integer>(
						pocetChunkov);
				for (int i = 0; i < pocetChunkov; i++) {
					aktualnyStavList.add(new Integer(0));
				}

				if (!subor.isDirectory() && subor.exists()) {
					// TODO zisti aktuálny stav sťahovania

				}

				aktualnyStav = new CopyOnWriteArrayList<Integer>(
						aktualnyStavList);

				soket.close();

				return null;
			}

			@Override
			protected void done() {
				// kontrola, či nenastala výnimka
				try {
					get();
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
						// TODO nastav progressBar
					}
					btnStartPause.setEnabled(true);
					btnStop.setEnabled(true);
					lblNazovSuboru.setText(ADRESAR_PRE_ULOZENIE
							+ subor.getName());
					lblNazovSuboru.setVisible(true);

					progressBar.setStringPainted(true);
					progressBar.setMinimum(0);
					// TODO nastav maximum pre progressBar
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
		KlientExecutor executor = new KlientExecutor(adresa, cisloPortu,
				pocetSoketov, subor, velkostSuboru, dlzkaChunku, aktualnyStav);
		executor.addPropertyChangeListener(this);
		executor.execute();
	}

	public void propertyChange(PropertyChangeEvent evt) {
		// TODO dokonč + skontroluj
		if ("progress" == evt.getPropertyName()) {
			int progress = (Integer) evt.getNewValue();
			progressBar.setValue(progress);
		}
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
