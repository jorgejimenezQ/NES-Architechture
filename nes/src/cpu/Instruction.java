package cpu;

public class Instruction {
	public String name;
	public Executable<CPU6502> instruction;
	public Executable<CPU6502> mode;
	public int cycles;
	public int bytes;
	public String modeName;

	public Instruction(String name, String modeName, Executable<CPU6502> instruction, Executable<CPU6502> mode,
			int cycles, int bytes) {
		this.name = name;
		this.instruction = instruction;
		this.mode = mode;
		this.cycles = cycles;
		this.bytes = bytes;
		this.modeName = modeName;
	}

	/**
	 * Called when making an instruction that is does not exist.
	 */
	public Instruction(String name) {
		this.name = "xxx";
		this.instruction = null;
		this.mode = null;
		this.cycles = 0;
		this.bytes = 0;

	}

	@Override
	public String toString() {
		return "Instruction: " + name + ", cycles=" + cycles + ", bytes=" + bytes;
	}
}
