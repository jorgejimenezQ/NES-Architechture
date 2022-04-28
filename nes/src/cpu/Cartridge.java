package cpu;

import helpers.NesRead;
import helpers.Hexdump;
public class Cartridge {

	private int[] prgMem;
	private int[] chrMem;
	private NesRead.Header header;
	private NametableMirror mirroring = NametableMirror.VERTICAL;
	private int mapperId;
	private Mapper mapper;

	/**
	 * <p>
	 * Nametable mirroring affects what is shown past the right and bottom edges of
	 * the current nametable. When mirroring is enabled for a particular axis
	 * (horizontal and/or vertical), the coordinates simply wrap around on the
	 * current nametable. A background "mirrored" in this way is repeated, not
	 * flipped. When mirroring is disabled, second nametable is used. There are four
	 * common combinations of mirroring. <a href=
	 * "https://wiki.nesdev.com/w/index.php/Mirroring#Nametable_Mirroring">Nesdev.com</a>
	 * </p>
	 * 
	 * @author jorgejimenez
	 *
	 */
	public static enum NametableMirror {

		VERTICAL, HORIZONTAL, SINGLE, FOUR_SCREEN;
	}

	public Cartridge(String filePath) {
		NesRead game = new NesRead(filePath);

		// Did we find a game file.
		if (game.gameExists()) {
			header = game.readHeader();
			int flag6 = header.mapper1;

			// Check if the iNes file contains a trainer and get rid of it.
			if (((flag6 >> 2) & 1) == 1)
				game.readBytes(new int[512]);

			// The mapper id's low 4 bits are given by bits 4-7 and the upper byble of the
			// mapper id is 4-7 of mapper 2.
			mapperId = (header.mapper2 & 0xf0) | (header.mapper1 >> 4);

			// Nametable mirroring.
			mirroring = ((header.mapper1 & 0b0000_0001) == 1) ? NametableMirror.VERTICAL : NametableMirror.HORIZONTAL;

			// Get the program ROM & the Char ROM.
			prgMem = new int[16384 * header.sizeOfPrgRom];
			chrMem = new int[8192 * header.sizeOfChrRom];

			game.readBytes(prgMem);
			game.readBytes(chrMem);

			// What mapper are we running.
			switch (mapperId) {
			case 0:
				mapper = new Mapper_0(header.sizeOfPrgRom, header.sizeOfChrRom);
				break;
			}

			return;
		}

		throw new IllegalArgumentException("The file was not found.");
	}

	/**
	 * <p>
	 * Performs a read originated from a {@code CPU6502}. TODO:
	 * </p>
	 * 
	 * @param addr
	 * @param readOnly
	 * @return The data at the address or -1 if not in the cartridge's memory range.
	 */
	public int cpuRead(int addr) {

		int mappedAddress = mapper.cpuRead(addr);

		if (mappedAddress != -1) {
			return prgMem[mappedAddress];
		}

		return -1;
	}

	/**
	 * <p>
	 * Performs a write originated from a {@code CPU6502}. TODO:
	 * </p>
	 * 
	 * @param addr
	 * @param readOnly
	 * @return -1 if not in the cartridge's memory range or 0 if the data was
	 *         written successfully.
	 */
	public int cpuWrite(int addr, int data) {
		int mappedAddress = mapper.cpuWrite(addr);

		if (mappedAddress != -1) {
			prgMem[mappedAddress] = data;
			return 0;
		}

		return -1;
	}

	/**
	 * <p>
	 * Reads using the {@code PPU2C02}'s bus. TODO:
	 * </p>
	 * 
	 * @param addr
	 * @param readOnly
	 * @return The data at the address or -1 if not in the cartridge's memory range.
	 */
	public int ppuRead(int addr) {
		int mappedAddress = mapper.ppuRead(addr);

		if (mappedAddress != -1) {
			return chrMem[mappedAddress];
		}

		return -1;
	}

	/**
	 * <p>
	 * Writes using the {@code PPU2C02}'s bus. TODO:
	 * </p>
	 * 
	 * @param addr
	 * @return -1 if not in the cartridge's memory range or 0 if the data was
	 *         written successfully.
	 */
	public int ppuWrite(int addr, int data) {
		int mappedAddress = mapper.ppuWrite(addr);

		if (mappedAddress != -1) {
			chrMem[mappedAddress] = data;
			return 0;
		}
		return -1;

	}
	// •––––––––––––––––––––––––––––––––––––
	// | GETTERS
	// •––––––––––––––––––––––––––––––––––––

	// •––––––––––––––––––––––––––––––––––––
	// | RETURN THE CHAR MEMORY
	// |
	// | - it is not used for the emulator
	// | - it is only for debugging.
	// •––––––––––––––––––––––––––––––––––––
	public int[] getChrMem() {
		return chrMem;
	}

	public NametableMirror getMirroring() {
		return mirroring;
	}

	@Override
	public String toString() {
		return "Cartridge: ";
	}

}
