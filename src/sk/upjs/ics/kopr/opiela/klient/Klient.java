package sk.upjs.ics.kopr.opiela.klient;

import javax.swing.SwingUtilities;
import sk.upjs.ics.kopr.opiela.klient.KlientGui;

public class Klient {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				new KlientGui();
			}

		});
	}

}
