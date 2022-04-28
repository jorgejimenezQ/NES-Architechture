package cpu;

/**
 * <p>
 * The CPU on the NES is addressed through the bus in the following way:
 * </p>
 * 
 * @author jorgejimenez
 *
 */
public class Bus {
	private int[] cpuRam;
	private PPU2C02 ppu;
	private Cartridge cart;

	// •–––––––––––––––––––––––––––•
	// | OAM
	// •–––––––––––––––––––––––––––•

	public int dmaPage = 0x00;
	public int dmaAddr = 0x00;
	public int dmaData = 0x00;
	public boolean dmaDummy = true;
	public boolean dmaTranfer = true;

	private int[] controllerState;
	public int[] controller;
	public CPU6502 cpu;

	public Bus(PPU2C02 ppu) {
		cpuRam = new int[0x800];
		this.ppu = ppu;
		this.cart = ppu.getCart();
		controller = new int[2];
		controllerState = new int[2];
		this.ppu.connect(this);
	}

	/**
	 * <p>
	 * TODO:
	 * </p>
	 * 
	 * @param addr
	 * @param readOnly
	 * @return
	 */
	public int cpuRead(int addr, boolean readOnly) {
		int data = 0x00;

		data = cart.cpuRead(addr);

		// If the address was in the cartridge's memory range
		// return the byte in that memory address. Otherwise check
		// main RAM or PPU I/O.
		if (data != -1) {
			return data;
		}

		data = 0x00;

		if (addr >= 0x0000 && addr <= 0x1fff) {
			// Reading from the main RAM
			data = cpuRam[addr & 0x07ff];

		} else if (addr >= 0x2000 && addr <= 0x3fff) {
			// Accessing the memory mapped I/O registers for the PPU
			data = ppu.cpuRead(addr & 0x0007, readOnly);

		} else if (addr >= 0x4016 && addr <= 0x4017) {
			data = (controllerState[addr & 0x0001] >> 7) & 1;
			// data = (controllerState[addr & 0x0001] & 0x80) > 0;
			controllerState[addr & 0x0001] <<= 1;
		}

		return data;
	}

	public void cpuWrite(int addr, int data) {

		// If the write to the cartridge was successful move on.
		// Otherwise try to write to main RAM or PPU I/O.
		if (cart.cpuWrite(addr, data) == 0) {
			return;
		}

		if (addr >= 0x0000 && addr <= 0x1fff) {

			cpuRam[addr & 0x07ff] = data;

		} else if (addr >= 0x2000 && addr <= 0x3fff) {

			ppu.cpuWrite(addr & 0x0007, data);

		} else if (addr == 0x4014) {

			dmaPage = data;
			dmaAddr = 0x00;
			dmaTranfer = true;

		} else if (addr >= 0x4016 && addr <= 0x4017) {

			controllerState[addr & 0x0001] = controller[addr & 0x0001];

		}

	}

	public int[] getCPUMem() {
		return cpuRam;
	}

	public void connect(CPU6502 cpu6502) {
		this.cpu = cpu6502;

	}
}