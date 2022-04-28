package helpers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author jorgejimenez
 *
 */
public class NesRead {

	private Header header;

	public static class Header {
		public int[] name = new int[4]; // 0-3: Constant $4E $45 $53 $1A ("NES" followed by MS-DOS end-of-file)
		public int sizeOfPrgRom; // 4 Size of PRG ROM in 16 KB units
		public int sizeOfChrRom; // 5 Size of CHR ROM in 8 KB units (Value 0 means the board uses CHR RAM)
		public int mapper1; // 6 Mapper, mirroring, battery, trainer.
		public int mapper2; // 7 Mapper, VS/Playchoice, NES 2.0.
		public int prgRamSize; // 7 PRG-RAM size (rarely used extension)
		public int tvsystem; // 8 
		public int tvsystem2; // 9 

		@Override
		public String toString() {
			return "(0 - 3): " + name[0] + " " + name[1] + " " + name[2] + " " + name[3] + " " + " (4) " + sizeOfPrgRom
					+ " (5) " + sizeOfChrRom + " (6) " + mapper1 + " (7) " + mapper2 + " (8) " + prgRamSize + " (9) "
					+ tvsystem + " (10) " + tvsystem2;
		}
	}

	private BufferedInputStream in;
	private boolean nesFileExists;

	public NesRead(String name) {
		try {
			File file = new File(name);
			if (file.exists()) {
				
				nesFileExists = true;
				FileInputStream fis = new FileInputStream(file);
				in = new BufferedInputStream(fis);
				return;
			}
			throw new IOException("The file does " + name + " not exist.");
		} catch (IOException ioe) {
			System.err.println("Could not open " + name);

		}
		 nesFileExists = false;
	}
	
	public boolean gameExists() {
		return nesFileExists;
	}

	public Header readHeader() {
		Header h = new Header();
		try {

			readBytes(h.name);

			h.sizeOfPrgRom = in.read();
			h.sizeOfChrRom = in.read();
			h.mapper1 = in.read();
			h.mapper2 = in.read();
			h.prgRamSize = in.read();
			h.tvsystem = in.read();
			h.tvsystem2 = in.read();
			readBytes(new int[5]);

//			System.out.println(Integer.toHexString(in.read()));
		} catch (Exception e) {
			// TODO: handle exception
		}
		return h;
	}

	public int readByte() {
		int b = 0;

		try {
			b = in.read();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return b;

	}

	public int[] readData() {
		int[] d = null;
		return d;
	}

	public void readBytes(int[] b) {
		try {
			int val = 0;
			for (int i = 0; i < b.length; i++) {
				val = in.read();

				b[i] = val;
				if (val == -1)
					return;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] arg) {
// /Users/jorgejimenez/Downloads/dkoe.nes
//		String fileName = "/Users/jorgejimenez/Downloads/dkoe.nes";
//
//		readHeader(fileName);
		String fileName2 = "/Users/jorgejimenez/Downloads/Donkey Kong (JU).nes";
		readHeader(fileName2);

//		Hexdump.print(prgRom);
//		Hexdump.print(chrRom);
		
		
		
		

	}

	private static void readHeader(String fileName) {
		NesRead in = new NesRead(fileName);
		Header h = in.readHeader();
		System.out.println(h);
		
		if (((h.mapper1 >> 2) & 1) == 1) {
			in.readBytes(new int[512]);
		}
		
		int[] prgRom = new int[16384 * h.sizeOfPrgRom];
		int[] chrRom = new int[8192 * h.sizeOfChrRom];
		
		
		
		in.readBytes(prgRom);
		in.readBytes(chrRom);
	}
	
}
