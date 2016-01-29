package sk.upjs.ics.kopr.opiela.klient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingWorker;

public class KlientExecutor extends SwingWorker<Void, Integer> {

	private final InetAddress adresa;

	private final int cisloPortu;

	private final int pocetSoketov;

	private final RandomAccessFile suborRAF;

	private final long velkostSuboru;

	private final long dlzkaKusku;

	private final CopyOnWriteArrayList<AtomicLong> aktualnyStav;

	private final AtomicLong progres;

	private final CopyOnWriteArrayList<Future<AtomicLong>> precitaneBajty;

	private final ExecutorService executor;

	public KlientExecutor(InetAddress adresa, int cisloPortu, int pocetSoketov,
			File subor, long velkostSuboru,
			CopyOnWriteArrayList<AtomicLong> aktualnyStav) {
		this.adresa = adresa;
		this.cisloPortu = cisloPortu;
		this.pocetSoketov = pocetSoketov;
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(subor, "rw");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.suborRAF = raf;
		this.velkostSuboru = velkostSuboru;
		this.aktualnyStav = aktualnyStav;

		dlzkaKusku = velkostSuboru / pocetSoketov;

		executor = Executors.newFixedThreadPool(pocetSoketov);

		precitaneBajty = new CopyOnWriteArrayList<Future<AtomicLong>>();

		progres = new AtomicLong(0);
	}

	@Override
	protected Void doInBackground() throws Exception {
		for (AtomicLong stav : aktualnyStav) {
			progres.addAndGet(stav.get());
			precitaneBajty.add(null);
		}

		setProgress((int) (100 * progres.get() / velkostSuboru));

		while (progres.get() < velkostSuboru && !Thread.interrupted()) {
			for (int i = 0; i < aktualnyStav.size(); i++) {
				long offset = i * dlzkaKusku
						+ (long) (aktualnyStav.get(i).get());
				long dlzka = dlzkaKusku - aktualnyStav.get(i).get();

				// posledný jeden bajt
				if (i == aktualnyStav.size() - 1
						&& (velkostSuboru % pocetSoketov) != 0) {
					dlzka = dlzkaKusku + (velkostSuboru % pocetSoketov)
							- aktualnyStav.get(i).get();
				}

				KlientJob job = new KlientJob(adresa, cisloPortu, suborRAF,
						offset, dlzka);
				precitaneBajty.set(i, executor.submit(job));
			}

			for (int i = 0; i < precitaneBajty.size(); i++) {
				aktualnyStav.get(i)
						.addAndGet(precitaneBajty.get(i).get().get());
				progres.addAndGet(precitaneBajty.get(i).get().get());
				setProgress((int) (100 * progres.get() / velkostSuboru));
			}
		}

		return null;
	}

	@Override
	protected void done() {
		try {
			// kontrola, či nenastala výnimka
			get();
			suborRAF.close();
		} catch (ExecutionException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException | CancellationException e) {
			// Úloha bola prerušená, netreba nič robiť
		}

		setProgress(100);
	}

}
