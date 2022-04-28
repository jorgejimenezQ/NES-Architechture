package cpu;

import javax.swing.plaf.ProgressBarUI;

/**
 * <p>
 * Mapper 0 has two configurations:
 * 
 * <pre>
 * * NROM-256 with 32 KiB PRG ROM and 8 KiB CHR ROM
 * * NROM-128 with 16 KiB PRG ROM and 8 KiB CHR ROM
 * </pre>
 * 
 * <br>
 * The program is mapped into $8000-$FFFF (NROM-256) or both $8000-$FFFF and
 * $CFFF - $FFFF (NROM-128).
 * </p>
 * 
 * @author jorgejimenez
 *
 */
public class Mapper_0 extends Mapper {

	public Mapper_0(int prgBanks, int chrBanks) {
		super(prgBanks, chrBanks);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cpu.Mapper#cpuRead(int)
	 */
	@Override
	int cpuRead(int addr) {

		return simpleCPUMap(addr);
	}

	@Override
	int cpuWrite(int addr) {

		return simpleCPUMap(addr);

	}


	@Override
	int ppuWrite(int addr) {
		// No mapping required for the PPU.
		if (addr >= 0x0000 && addr <= 0x1FFF)
			if (chrBanks == 0) // If the flag 5 (sizeOfChrRom == 0), use as RAM
				return addr;
		return -1;
	}

	@Override
	int ppuRead(int addr) {
		// No mapping required for the PPU.
		if (addr >= 0x0000 && addr <= 0x1FFF)
			return addr;
		return -1;
	}

	// Is the address between $8000 and $FFFF?
	private int simpleCPUMap(int addr) {
		if (addr >= 0x8000 && addr <= 0xffff) {

			int newAddress = addr & (prgBanks > 1 ? 0x7FFF : 0x3FFF);

			return newAddress;
		}
		return -1;
	}
}
