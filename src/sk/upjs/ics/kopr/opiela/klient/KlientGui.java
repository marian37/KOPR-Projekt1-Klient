package sk.upjs.ics.kopr.opiela.klient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

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

	private static final int AKCIA_SPUSTENIE = 0;

	private static final int AKCIA_POKRACOVANIE = 1;

	private static final int AKCIA_PRERUSENIE = 2;

	private static final int AKCIA_UKONCENIE = 3;

	private final JFrame frame = new JFrame("Klient");

	private final JLabel lblAdresaServeru = new JLabel("Adresa serveru:");

	private final JTextField txtAdresaServeru = new JTextField("localhost");

	private final JLabel lblPort = new JLabel("Port:");

	private final JTextField txtPort = new JTextField("5000");

	private final JLabel lblPocetSoketov = new JLabel("Počet soketov:");

	private final JSpinner pocetSoketovSpinner = new JSpinner(
			new SpinnerNumberModel(4, MIN_SOCKETS, MAX_SOCKETS, 1));

	private final JButton btnPotvrdit = new JButton("Potvrdiť");

	private final JLabel lblNazovSuboru = new JLabel();

	private final JProgressBar progressBar = new JProgressBar();

	private final JButton btnStartPause = new JButton("Spustiť sťahovanie");

	private final JButton btnStop = new JButton("Zrušiť sťahovanie");

	private InetAddress adresa;

	private int cisloPortu;

	private int pocetSoketov;

	private File subor;

	private long velkostSuboru;

	private final File info = new File(ADRESAR_PRE_ULOZENIE + "info.download");;

	private CopyOnWriteArrayList<AtomicLong> aktualnyStav;

	private int progresPoNacitani;

	private int akcia = AKCIA_SPUSTENIE;

	private KlientExecutor executor;

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

				List<AtomicLong> aktualnyStavList = new ArrayList<AtomicLong>(
						pocetSoketov);
				for (int i = 0; i < pocetSoketov; i++) {
					aktualnyStavList.add(new AtomicLong(0));
				}

				if (!subor.isDirectory() && subor.exists()) {
					if (!info.isDirectory() && info.exists()) {
						aktualnyStavList = nacitajAktualnyStav();
					} else {
						// už je stiahnutý celý súbor
						long dlzkaKusku = velkostSuboru / pocetSoketov;
						for (int i = 0; i < aktualnyStavList.size(); i++) {
							if (i == aktualnyStavList.size() - 1) {
								aktualnyStavList.set(i, new AtomicLong(
										dlzkaKusku + velkostSuboru
												% pocetSoketov));
							}
							aktualnyStavList.set(i, new AtomicLong(dlzkaKusku));
						}
						progresPoNacitani = 100;
						setProgress(progresPoNacitani);
					}
				}

				aktualnyStav = new CopyOnWriteArrayList<AtomicLong>(
						aktualnyStavList);

				soket.close();

				return null;
			}

			@Override
			protected void done() {
				// kontrola, či nenastala výnimka
				try {
					get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(frame,
							"Zlyhalo spojenie so serverom.", "Varovanie",
							JOptionPane.WARNING_MESSAGE);
				}

				progressBar.setVisible(false);
				progressBar.setIndeterminate(false);

				if (subor != null) {
					progressBar.setStringPainted(true);
					progressBar.setMinimum(0);
					progressBar.setMaximum(100);

					if (!subor.isDirectory() && subor.exists()) {
						if (progresPoNacitani == 100) {
							btnStartPause.setText("Ukončiť program");
							akcia = AKCIA_UKONCENIE;
						} else {
							btnStartPause.setText("Pokračovať v sťahovaní");
							akcia = AKCIA_POKRACOVANIE;
						}
						progressBar.setValue(progresPoNacitani);
						progressBar.setVisible(true);
					}

					btnStartPause.setEnabled(true);
					btnStop.setEnabled(true);
					lblNazovSuboru.setText(ADRESAR_PRE_ULOZENIE
							+ subor.getName());
					lblNazovSuboru.setVisible(true);
				} else {
					txtAdresaServeru.setEnabled(true);
					txtPort.setEnabled(true);
					pocetSoketovSpinner.setEnabled(true);
					btnPotvrdit.setEnabled(true);
				}
			}

			private List<AtomicLong> nacitajAktualnyStav() {
				List<AtomicLong> list = new ArrayList<AtomicLong>();
				try {
					ObjectInputStream ois = new ObjectInputStream(
							new FileInputStream(info));
					pocetSoketov = ois.readInt();
					AtomicLong progres = new AtomicLong(0);
					for (int i = 0; i < pocetSoketov; i++) {
						list.add(new AtomicLong(ois.readLong()));
						progres.addAndGet(list.get(i).get());
					}
					ois.close();

					progresPoNacitani = (int) (100 * progres.get() / velkostSuboru);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				return list;
			}
		};

		testSpojenia.execute();
	}

	private void btnStartPauseActionPerformed() {
		switch (akcia) {
		case AKCIA_SPUSTENIE:
		case AKCIA_POKRACOVANIE:
			progressBar.setVisible(true);
			btnStartPause.setText("Prerušiť sťahovanie");
			executor = new KlientExecutor(adresa, cisloPortu, pocetSoketov,
					subor, velkostSuboru, aktualnyStav);
			executor.addPropertyChangeListener(this);
			executor.execute();
			akcia = AKCIA_PRERUSENIE;
			break;

		case AKCIA_PRERUSENIE:
			ulozAktualnyStav();
			break;

		default:
			// AKCIA_UKONCENIE
			boolean uspesne = true;
			if (!info.isDirectory() && info.exists()) {
				uspesne = info.delete();
			}
			if (uspesne) {
				frame.dispatchEvent(new WindowEvent(frame,
						WindowEvent.WINDOW_CLOSING));
			} else {
				JOptionPane.showMessageDialog(frame,
						"Nepodarilo sa zmazať súbor " + info.getName(),
						"Varovanie", JOptionPane.WARNING_MESSAGE);
			}
			break;
		}

	}

	private void ulozAktualnyStav() {
		SwingWorker<Void, Void> ukladanie = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				executor.cancel(true);

				ObjectOutputStream oos = new ObjectOutputStream(
						new FileOutputStream(info));

				oos.writeInt(aktualnyStav.size()); // počet záznamov
				for (AtomicLong stav : aktualnyStav) {
					oos.writeLong(stav.get()); // všetky záznamy
				}

				oos.flush();
				oos.close();

				return null;
			}

			@Override
			protected void done() {
				try {
					// kontrola, či nenastala výnimka
					get();
				} catch (InterruptedException | ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// zatvorenie okna
				frame.dispatchEvent(new WindowEvent(frame,
						WindowEvent.WINDOW_CLOSING));
			}
		};
		ukladanie.execute();
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress" == evt.getPropertyName()) {
			int progress = (Integer) evt.getNewValue();
			progressBar.setValue(progress);
			if (progress == 100) {
				executor.cancel(true);
				akcia = AKCIA_UKONCENIE;
				btnStartPause.setText("Ukončiť program");
				btnStartPause.setToolTipText("Súbor bol úspešne stiahnutý.");
			}
		}
	}

	private void btnStopActionPerformed() {
		// vymaže všetko stiahnuté
		boolean uspesne = true;
		boolean uspesneInfo = true;
		if (subor != null && !subor.isDirectory() && subor.exists()) {
			uspesne = subor.delete();
		}
		if (info != null && !info.isDirectory() && info.exists()) {
			uspesneInfo = info.delete();
		}
		if (!uspesne) {
			JOptionPane
					.showMessageDialog(frame, "Nepodarilo sa zmazať súbor "
							+ subor.getName(), "Varovanie",
							JOptionPane.WARNING_MESSAGE);
		} else {
			if (!uspesneInfo) {
				JOptionPane.showMessageDialog(frame,
						"Nepodarilo sa zmazať súbor " + info.getName(),
						"Varovanie", JOptionPane.WARNING_MESSAGE);
			} else {
				frame.dispatchEvent(new WindowEvent(frame,
						WindowEvent.WINDOW_CLOSING));
			}
		}
	}

}
