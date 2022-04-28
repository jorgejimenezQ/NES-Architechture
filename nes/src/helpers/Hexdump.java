package helpers;

/**
 * The {@code Hexdump} class provides an API for displaying the content of an
 * integer array in hexadecimal.
 * 
 * @author jorgejimenez
 *
 */
public class Hexdump {

	public static void print(int[] a) {
		print(0, a.length, a);
	}

	public static void print(int width, int[] a) {
		System.out.println(hexString(0, a.length, width, a));
	}

	public static void print(int start, int end, int[] a) {
		System.out.println(hexString(start, end, a));
	}

	public static void print(int start, int end, int width, int[] a) {
		System.out.println(hexString(start, end, width, a));
	}

	public static String hexString(int[] a) {
		return hexString(0, a.length, 16, a);
	}

	public static String hexString(int start, int end, int[] a) {
		return hexString(start, end, 16, a);
	}

	/**
	 * Returns the contents of an {@code Array} of integers in hexadecimal.
	 * 
	 * @param start
	 *            The
	 * @param end
	 * @param width
	 * @param a
	 * @return a {@code String} containing the contents of the array.
	 */
	public static String hexString(int start, int end, int width, int[] a) {

		validateStartEnd(start, end, a.length);
		StringBuilder sb = new StringBuilder();

		int bytesPerLine = width;
		int addr = start;
		sb.append(printHexPadded(start, 4) + ":   ");
		for (int i = start; i < end; i++) {

			if ((i % bytesPerLine) == (bytesPerLine - 1)) {
				addr += bytesPerLine;
				sb.append(" " + printHexPadded(a[i], 2));
				sb.append("\n");
				sb.append(printHexPadded(addr, 4) + ":   ");
				continue;
			}

			sb.append(" " + printHexPadded(a[i], 2));
		}

		return sb.toString();
	}

	private static void validateStartEnd(int start, int end, int length) {
		if (start < 0 || end < 0)
			throw new IllegalArgumentException("The \"start\" and \"end\" parameters cannot be neganitive.");
		if (start > end)
			throw new IllegalArgumentException("The \"start\" of the array must be before the \"end\".");
		if (start > length || end > length)
			throw new IllegalArgumentException(
					"The \"start\" and \"end\" parameters are not within the size of the array.");

	}

	public static String printHexPadded(int n, int padding) {
		return String.format("%0" + padding + "X", n);
	}

	// Does nothing
	public static void outToFile() {
		// TODO:
	}
}
