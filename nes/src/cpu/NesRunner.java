package cpu;

public class NesRunner {
	private CPU6502 cpu;
	private PPU2C02 ppu;
	private Cartridge cart;
	private Bus bus;
	private String filename;
	public int systemClock = 0;

	public NesRunner() {
		// filename = "/Users/jorgejimenez/Downloads/Donkey Kong (JU).nes";
		// filename = "/Users/jorgejimenez/Downloads/Donkey Kong.nes";
		 filename = "/Users/jorgejimenez/Downloads/Super Mario Bros..nes";
		// filename = "/Users/jorgejimenez/Downloads/Ice Climber.nes";
//		filename = "/Users/jorgejimenez/Downloads/nestest.nes";
		// filename = "/Users/jorgejimenez/Downloads/official_only.nes";
		// filename = "/Users/jorgejimenez/Downloads/1942 (PC10).nes";

		cart = new Cartridge(filename);
		ppu = new PPU2C02(cart);
		bus = new Bus(ppu);
		cpu = new CPU6502(bus);

		cpu.reset();
	}

	public void clock() {

		ppu.clock();

		if (systemClock % 3 == 0) {

			if (bus.dmaTranfer) {
				if (bus.dmaDummy) {
					if (systemClock % 2 == 1) {
						bus.dmaDummy = false;
					}
				} else {
					if (systemClock % 2 == 0) {
						bus.dmaData = bus.cpuRead((bus.dmaPage << 8) | bus.dmaAddr, false);
						bus.dmaData &= 0xff;
					} else {
						ppu.OAM[bus.dmaAddr] = bus.dmaData & 0xff;
						bus.dmaAddr++;
						bus.dmaAddr &= 0xff;
						if (bus.dmaAddr == 0x00) {
							bus.dmaTranfer = false;
							bus.dmaDummy = true;
						}

					}
				}
			} else {
				cpu.clock();
			}
			// cpu.toString();
			// System.out.println(cpu);
		}

		if (ppu.nmi) {
			ppu.nmi = false;
			// System.out.println(cpu.getProgramCounter());
			cpu.NMI();
		}
		systemClock++;
	}

	public CPU6502 getCpu() {
		return cpu;
	}

	public PPU2C02 getPpu() {
		return ppu;
	}

	public Cartridge getCart() {
		return cart;
	}

	public Bus getBus() {
		return bus;
	}

	public void reset() {
		cart = new Cartridge(filename);
		ppu = new PPU2C02(cart);
		bus = new Bus(ppu);
		cpu = new CPU6502(bus);

		cpu.reset();

	}
}
