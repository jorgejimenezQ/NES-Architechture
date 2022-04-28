package cpu;

public class Nes {

	public int systemClock = 0;
	private PPU2C02 ppu;
	private CPU6502 cpu;
	private Cartridge cart;

	public void main(String[] args) {
		String filepath = "/Users/jorgejimenez/Downloads/Donkey Kong (JU).nes";
		cart = new Cartridge(filepath);
		ppu = new PPU2C02(cart);
		cpu = new CPU6502(new Bus(ppu));
		cpu.reset();

//		systemClock = cpu.cycle * 3;
//		ppu.cycle = systemClock;
		while (true) {

		}
	}

	public void clock() {
		ppu.clock();

		if (systemClock % 3 == 0) {
			cpu.clock();

			System.out.println(cpu);
		}

		systemClock++;
	}
}
