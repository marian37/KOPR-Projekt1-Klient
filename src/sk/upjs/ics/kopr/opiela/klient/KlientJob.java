package sk.upjs.ics.kopr.opiela.klient;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Callable;

public class KlientJob implements Callable<Integer> {

	private final InetAddress adresa;

	private final int cisloPortu;

	private final RandomAccessFile suborRAF;

	private final int offset;

	private final int dlzka;

	public KlientJob(InetAddress adresa, int cisloPortu,
			RandomAccessFile suborRAF, int offset, int dlzka) {
		this.adresa = adresa;
		this.cisloPortu = cisloPortu;
		this.suborRAF = suborRAF;
		this.offset = offset;
		this.dlzka = dlzka;
	}

	@Override
	public Integer call() throws Exception {
		System.out.println("Spustam job: " + offset + " " + dlzka);

		Socket soket = new Socket(adresa, cisloPortu);
		soket.setKeepAlive(true);

		PrintWriter pw = new PrintWriter(soket.getOutputStream());
		pw.println("data");
		pw.println(offset);
		pw.println(dlzka);
		pw.flush();

		InputStream is = soket.getInputStream();
		byte[] bytes = new byte[dlzka];

		int precitane = is.read(bytes, 0, dlzka);

		suborRAF.write(bytes, offset, dlzka);

		soket.close();

		return precitane;
	}

}
