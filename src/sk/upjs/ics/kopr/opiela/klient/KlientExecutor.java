package sk.upjs.ics.kopr.opiela.klient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.SwingWorker;

public class KlientExecutor extends SwingWorker<Integer, Integer> {

	private final InetAddress adresa;

	private final int cisloPortu;

	private final int pocetSoketov;

	private final File subor;

	private final RandomAccessFile suborRAF;

	private final long velkostSuboru;

	private final int dlzkaChunku;

	private final CopyOnWriteArrayList<Integer> aktualnyStav;

	private List<Future<Integer>> precitaneBajty;

	private ExecutorService executor;

	public KlientExecutor(InetAddress adresa, int cisloPortu, int pocetSoketov,
			File subor, long velkostSuboru, int dlzkaChunku,
			CopyOnWriteArrayList<Integer> aktualnyStav) {
		this.adresa = adresa;
		this.cisloPortu = cisloPortu;
		this.pocetSoketov = pocetSoketov;
		this.subor = subor;
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(subor, "rw");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.suborRAF = raf;
		this.velkostSuboru = velkostSuboru;
		this.dlzkaChunku = dlzkaChunku;
		this.aktualnyStav = aktualnyStav;
		executor = Executors.newFixedThreadPool(pocetSoketov);

		precitaneBajty = new ArrayList<Future<Integer>>();
	}

	@Override
	protected Integer doInBackground() throws Exception {
		// TODO setProgress();

		// TODO offset a dlzka pre pokračovanie v sťahovaní

		for (int i = 0; i < aktualnyStav.size(); i++) {
			int offset = i * dlzkaChunku + aktualnyStav.get(i);
			int dlzka = dlzkaChunku - aktualnyStav.get(i);

			if (velkostSuboru - offset < dlzka) {
				dlzka = (int) velkostSuboru - offset;
			}

			KlientJob job = new KlientJob(adresa, cisloPortu, suborRAF, offset,
					dlzka);
			precitaneBajty.add(executor.submit(job));
		}

		return null;
	}

	@Override
	protected void done() {
		// TODO
		try {
			get();
			for (int i = 0; i < precitaneBajty.size(); i++) {
				System.out.println(i + " : " + precitaneBajty.get(i));
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO 100% do progressBaru
		setProgress(100);
	}

}
