package no.nav.sbl.dialogarena.types;

/**
 * Implementeres av komponenter skal overvåkes av selftest
 */
public interface Pingable {
	
	Ping ping();
	
	final class Ping {
		
		public final boolean vellykket;
		public final String komponent;
		public final Exception aarsak;
		public Long tidsbruk;
		
		private Ping(boolean success, String komponent, Exception aarsak) {
			this.vellykket = success;
			this.komponent = komponent;
			this.aarsak = aarsak;
		}

		public static Ping lyktes(String komponent) {
			return new Ping(true, komponent, null);
		}
		
		public static Ping feilet(String komponent, Exception aarsak) {
			return new Ping(false, komponent, aarsak);
		}
		
	}

}
