package cpu;

/**
 * Depending on the game, an NES cartridge contains RAM and ROM for both CPU PPU
 * busses. Often these chips are much large than 64K and depend on dedicated
 * circuitry inside the cartridge to map smaller areas of the chips into the two
 * address spaces. The game program controls the banks by writing to registers
 * in the dedicated circuitry. There were many different Memory Management
 * Controllers (MMCs) used in cartridges over the years. Most were designed to
 * be program configurable so that a single cartridge configuration could be
 * used for a variety of games.
 * 
 * @author jorgejimenez
 *
 */
public abstract class Mapper {

	protected int prgBanks;
	protected int chrBanks;

	public Mapper(int prgBanks, int chrBanks) {
		this.prgBanks = prgBanks;
		this.chrBanks = chrBanks;
	}

	/**
	 * <p>
	 * Takes the address and maps it to the correct address on the cartridge.
	 * </p>
	 * 
	 * @param addr
	 * @return The mapped address or -1 if not in the range for the mapper.
	 */
	abstract int cpuRead(int addr);

	/**
	 * <p>
	 * Takes the address and maps it to the correct address on the cartridge.
	 * </p>
	 * 
	 * @param addr
	 * @return The mapped address or -1 if not in the range for the mapper.
	 */
	abstract int cpuWrite(int addr);

	/**
	 * <p>
	 * Takes the address and maps it to the correct address on the cartridge.
	 * </p>
	 * 
	 * @param addr
	 * @return The mapped address or -1 if not in the range for the mapper.
	 */
	abstract int ppuWrite(int addr);

	/**
	 * <p>
	 * Takes the address and maps it to the correct address on the cartridge.
	 * </p>
	 * 
	 * @param addr
	 * @return The mapped address or -1 if not in the range for the mapper.
	 */
	abstract int ppuRead(int addr);
}
