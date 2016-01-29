package sk.upjs.ics.kopr.opiela.klient;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

public class KlientJob implements Callable<AtomicLong> {

	private static final int MAX_DLZKA = 1000000;

	private final InetAddress adresa;

	private final int cisloPortu;

	private final RandomAccessFile suborRAF;

	private final long offset;

	private final int dlzka;

	public KlientJob(InetAddress adresa, int cisloPortu,
			RandomAccessFile suborRAF, long offset, long dlzka) {
		this.adresa = adresa;
		this.cisloPortu = cisloPortu;
		this.suborRAF = suborRAF;
		this.offset = offset;

		int dlzkaPom = (int) dlzka;
		if (dlzkaPom > MAX_DLZKA) {
			dlzkaPom = MAX_DLZKA;
		}
		this.dlzka = dlzkaPom;
	}

	@Override
	public AtomicLong call() throws Exception {
		if (dlzka <= 0) {
			return new AtomicLong(0);
		}

		Socket soket = new Socket(adresa, cisloPortu);

		PrintWriter pw = new PrintWriter(soket.getOutputStream());
		pw.println("data");
		pw.println(offset);
		pw.println(dlzka);
		pw.flush();

		InputStream is = soket.getInputStream();
		byte[] bytes = new byte[dlzka];

		final int precitane = is.read(bytes);

		synchronized (suborRAF) {
			suborRAF.seek(offset);
			suborRAF.write(bytes, 0, precitane);
		}

		soket.close();

		return new AtomicLong(precitane);
	}
}
