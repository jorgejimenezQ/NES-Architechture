
package cpu;

import edu.princeton.cs.algs4.HexDump;
import helpers.Hexdump;

/**
 * <p>
 * This class represents a 6502 CPU interfaced with the <code>Bus</code> class.
 * The <code>Bus</code> class provides a way to interface with other modules
 * i.e., ROM, RAM etc. The 6502 uses 16-bit bus and 8-bit data lines. The class
 * uses a <a href =
 * "http://nesdev.com/NES%20emulator%20development%20guide.txt">Microcode-based
 * 6502 opcode interpretation</a>.
 * </p>
 * 
 * 
 * @author jorgejimenez
 *
 */
public class CPU6502 {

	// •–––––––––––––––––––––––––––––––––––––––––––––––
	// |DEBUGGING
	// •–––––––––––––––––––––––––––––––––––––––––––––––
	public static boolean STOP_RUN = false;

	public static boolean START_CLOCK = false;

	// Array containing the instruction set.
	// The opcode is the used to access the instruction i.e., $0a instruction
	// ASL.

	public Instruction[][] instructions = new Instruction[0x0f + 1][0x0f + 1];
	private Bus bus;

	// A pointer to the Instruction currently being executed.
	private Instruction currentInstruction = null;

	// The 6502 registers
	// 16-bit register pointst to the next instruction to be executed.
	private int programCounter = 0x00;
	// 256 stack between $0100 and $01ff
	private int stackPointer = 0x00;
	// The 8 bit index register is most commonly used to hold counters or
	// offsets
	// for accessing memory.
	private int x = 0x00;
	// The Y register is similar to the X register in that it is available for
	// holding counter or offsets memory access.
	private int y = 0x00;
	// The 8 bit accumulator is used all arithmetic and logical operations (with
	// the
	// exception of increments and decrements).
	private int a = 0x00;
	// As instructions are executed a set of processor flags are set or clear to
	// record the results of the operation. This flags and some additional
	// control
	// flags are held in a special status register. Each flag has a single bit
	// within the register.
	private int status = 0x00;

	// Keep track of the number of cycles.
	public int cycle = 0;

	// Total number of cycles.
	int totalCycles = 0;

	// Store the addresses passed to the current instruction.
	public int handlingData = 0x0000;

	// •–––––––––––––––––––––––––––––––––––––––•
	// | DEBUGGING
	// •–––––––––––––––––––––––––––––––––––––––•
	private boolean DEBUG_FILL2007_STOP = false;
	private boolean DEBUG_FILL2007_END_STOP = false;

	public CPU6502(Bus bus) {

		// connect to the bus
		this.bus = bus;
		bus.connect(this);
		getInstructionsSet();
	}

	public Bus getBus() {
		return this.bus;
	}

	/**
	 * Gets and executes the next instructions.
	 * 
	 * @param mode
	 * @param reg
	 */
	public void fetchInstruction(int opcode) {

		int bitmask = 0xf0;
		int ms = (opcode & bitmask) >>> 4;
		bitmask = 0x0f;
		int ls = (opcode & bitmask);

		if ((ms < 0 && ms > 0x0f) || (ls < 0 && ls > 0x0f))
			throw new IllegalArgumentException();

		currentInstruction = instructions[ms][ls];

		cycle = currentInstruction.cycles;

		// ––––––––––––––––––––––––––––––––––––––––
		// DEBUG

		if (this.programCounter > 0xf1c8 && this.programCounter <= 0xf1ce) {
			this.DEBUG_FILL2007_STOP = true;
		}

		if (this.programCounter >= 0xf1d7) {
			this.DEBUG_FILL2007_END_STOP = true;
		}

		if (this.programCounter >= 0xc7c2) {
			this.DEBUG_FILL2007_END_STOP = true;
		}
		if (this.programCounter >= 0xf213 && this.programCounter <= 0xf220) {
			this.DEBUG_FILL2007_END_STOP = true;
		}
		if (this.programCounter >= 0xf21c && this.programCounter <= 0xf220) {
			this.DEBUG_FILL2007_END_STOP = true;
		}

		if (this.programCounter >= 0xf202 && this.programCounter <= 0xf207) {
			this.DEBUG_FILL2007_END_STOP = true;
		}

		if (this.programCounter == 0xf21c) {
			this.DEBUG_FILL2007_END_STOP = true;
		}
		if (this.programCounter == 0xcbd9) {
			this.DEBUG_FILL2007_END_STOP = true;
		}

		// ––––––––––––––––––––––––––––––––––––––––

		// Run mode then instruction. and keep track of cycles. If both return 1
		// add one
		// cycle to the cycle counter.
		int m = currentInstruction.mode.execute(this);
		int i = currentInstruction.instruction.execute(this);

		cycle += (m & i);

		totalCycles += cycle;

	}

	public boolean complete() {
		return cycle == 0;
	}

	/**
	 * Get a staus flag.
	 * 
	 * @param f
	 * @return true if the flag is set.
	 */
	public boolean getFlag(Flag f) {

		return (status & f.getFlag()) > 0 ? true : false;
	}

	/**
	 * Sets the bit.
	 * 
	 * @param f
	 */
	public void setFlag(Flag f) {
		status |= f.getFlag();
	}

	/**
	 * Sets the flag to zero.
	 * 
	 * @param f
	 */
	public void clearFlag(Flag f) {
		status &= ~(f.getFlag());
	}

	/**
	 * <p>
	 * Resets the microprocessor through an initialization sequence lasting
	 * seven clock cycles. Then the interrupt mask flag is set, the decimal mode
	 * is cleared (This behavior is ignored in this emulator of the 6502), and
	 * the program counter is loaded with the restart vector from locations FFFC
	 * (low byte) and FFFD(high byte). THis is the start location for program
	 * control.
	 * </p>
	 * 
	 */
	public void reset() {
		// clear the statusRegister.
		this.status = 0x00 | Flag.UNUSED.getFlag();

		// Get the location of the PC from the next two addresses.
		int lowByte = read(0xfffc);
		int highByte = read(0xfffd);

		programCounter = (highByte << 8) | lowByte;

		x = 0x00;
		y = 0x00;
		a = 0x00;
		stackPointer = 0xfd;
		handlingData = 0x00;
		cycle = 8;

		totalCycles = cycle;
		// System.out.println(String.format("%4x", (highByte << 8) | lowByte));
	}

	// TODO:
	/**
	 * <p>
	 * If the interrupt flag in the processor status register is zero, the
	 * interrupt sequence begins. <strong>The program counter and processor
	 * status register are stored in the stack</strong>. The microprocessor will
	 * set the interrupt mask flag high so that no further IRQ's may occur. At
	 * the end of this cycle, the program counter low byte will be loaded from
	 * address FFFE, and program counter high byte form location FFFF, thus
	 * transferring the program control to the memory vector located at these
	 * addresses.
	 * </p>
	 */
	public void IRQ() {
		if (!getFlag(Flag.INTERRUPT_DISABLE)) {
			// Store PC
			write(0x0100 + getStackPointer(), (getProgramCounter() >> 8) & 0x00ff);
			decSP();
			write(0x0100 + getStackPointer(), getProgramCounter() & 0x00ff);
			decSP();

			// Store Status Reg.

			clearFlag(Flag.BREAK);
			setFlag(Flag.UNUSED);
			setFlag(Flag.INTERRUPT_DISABLE);
			write(0x0100 + getStackPointer(), getStatus());
			decSP();

			// Load the PC from 0xfffe and 0xffff
			int lowbyte = read(0xfffe);
			int highbyte = read(0xffff);
			setProgramCounter((highbyte << 8) | lowbyte);

			cycle = 7;
		}
	}

	/**
	 * <p>
	 * The program counter is loaded with interrupt vector from location FFFA
	 * (low byte) and FFFB (high byte), thereby transferring program control the
	 * the non-maskable interrupt routine.
	 * </p>
	 */
	public void NMI() {
		// System.out.println(Hexdump.printHexPadded(this.getProgramCounter(),
		// 4));

		// Store PC
		write(0x0100 + getStackPointer(), (getProgramCounter() >> 8) & 0x00ff);
		decSP();
		write(0x0100 + getStackPointer(), getProgramCounter() & 0x00ff);
		decSP();

		// Store Status Reg.

		clearFlag(Flag.BREAK);
		setFlag(Flag.UNUSED);
		setFlag(Flag.INTERRUPT_DISABLE);
		write(0x0100 + getStackPointer(), getStatus());
		decSP();

		// Load the PC from 0xfffe and 0xffff
		int lowbyte = read(0xfffa);
		int highbyte = read(0xfffb);

		setProgramCounter((highbyte << 8) | lowbyte);

		cycle = 8;

	}

	public void clock() {
		// TODO:
		if (cycle == 0) {
			this.setFlag(Flag.UNUSED);
			fetchInstruction(read(getProgramCounter()));
			incPC();

		}
		cycle--;
	}

	/**
	 * <p>
	 * Reads from the RAM or ROM connected by the {@code Bus}.
	 * </p>
	 * 
	 * @param addr
	 * @return
	 */
	public int read(int addr) {
		return bus.cpuRead(addr, false);
	}

	/**
	 * <p>
	 * Writes to the RAM connected by the {@code Bus}.
	 * </p>
	 * 
	 * @param addr
	 * @return
	 */
	public void write(int addr, int data) {
		bus.cpuWrite(addr, data);
	}

	public int getProgramCounter() {
		return programCounter;
	}

	public Instruction getCurrentInstruction() {
		return this.currentInstruction;
	}

	public void setProgramCounter(int programCounter) {
		this.programCounter = programCounter;
	}

	public int getStackPointer() {
		return stackPointer;
	}

	public void setStackPointer(int stackPointer) {
		this.stackPointer = stackPointer;
	}

	public int getX() {
		return x;
	}

	public void setX(int registerX) {
		this.x = registerX % 256;
	}

	public int getY() {
		return y;
	}

	public void setY(int registerY) {
		this.y = registerY % 256;
	}

	/**
	 * Returns the accumulator.
	 * 
	 * @return
	 */
	public int getA() {
		return a;
	}

	/**
	 * Sets the accumulator.
	 * 
	 * @param registerA
	 */
	public void setA(int registerA) {
		this.a = registerA % 256;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int statusRegister) {
		this.status = statusRegister;
	}

	public int getCycles() {
		return cycle;
	}

	public void setCycles(int cycles) {
		this.cycle = cycles;
	}

	public int getAbsoluteAddress() {
		return handlingData;
	}

	public void setAbsoluteAddress(int absoluteAddress) {
		this.handlingData = absoluteAddress;
	}

	// @Override
	// public String toString() {
	// Instruction ins = (this.currentInstruction != null) ?
	// this.currentInstruction : new Instruction("xxx");
	// return "\nLast Instruction executed:" + ins.name + " " + ins.modeName
	// + "\nX: " + String.format("0x%02x", x) + " Y: " + String.format("0x%02x",
	// y) + " A: "
	// + String.format("0x%02x", a) + "\nSP: " + String.format("0x%02x",
	// this.stackPointer) + " PC: "
	// + String.format("0x%02x", programCounter) + "\n\nNV-BDIZC\n"
	// + String.format("%8s", Integer.toBinaryString(status)).replace(' ', '0')
	// + "\nHandling Data: "
	// + String.format("0x%02x", handlingData);
	// }

	@Override
	public String toString() {

		// C5F5 A2 00 LDX #$00 A:00 X:00 Y:00 P:24 SP:FD PPU: 0, 30 CYC:10
		StringBuilder sb = new StringBuilder();
		//
		// if (this.currentInstruction != null) {
		// sb.append(getProgramCounter() + " ");
		// for (int i = 0; i < currentInstruction.bytes; i++) {
		// sb.append(read(getProgramCounter() + 1) + " ");
		// }
		// sb.append(" ");
		// sb.append(((currentInstruction.modeName == "implied") ? "#$" :"$") +
		// );
		// }
		sb.append((this.currentInstruction != null) ? this.currentInstruction.name : "xxxx");
		sb.append(" ");
		sb.append((this.currentInstruction != null) ? this.currentInstruction.modeName : "xxxx");
		sb.append(" ");
		sb.append(" PC: ");
		sb.append(String.format("0x%02x", programCounter));
		sb.append("\nX: ");
		sb.append(String.format("0x%02x", x));
		sb.append(" Y: ");
		sb.append(String.format("0x%02x", y));
		sb.append(" A: ");
		sb.append(String.format("0x%02x", a));
		sb.append('\n');
		sb.append("SP: ");
		sb.append(String.format("0x%02x", this.stackPointer));

		sb.append('\n');

		sb.append("NV-BDIZC Fetching Data: " + String.format("0x%02x", handlingData));
		sb.append('\n');
		sb.append(String.format("%8s", Integer.toBinaryString(status)).replace(' ', '0'));
		sb.append('\n');
		sb.append("Cycles: " + this.totalCycles);
		return sb.toString();
	}

	/**
	 * Increments the Program counter by one.
	 */
	public void incPC() {
		this.programCounter++;
	}

	/**
	 * Decreases the program counter by one.
	 */
	public void decPC() {
		programCounter--;
	}

	/**
	 * Decreases the stack pointer by one.
	 */
	public void decSP() {
		stackPointer--;
	}

	/**
	 * Increases the stack pointer by one.
	 */
	public void incSP() {
		stackPointer++;
	}

	// O------------------------x
	// | HELPER
	// | FUNCTIONS
	// O------------------------x

	/**
	 * Get an instruction from an opcode.
	 * 
	 * @param opcode
	 * @return
	 */
	private Instruction getInstruction(int opcode) {
		int bitmask = 0xf0;
		int ms = (opcode & bitmask) >>> 4;
		bitmask = 0x0f;
		int ls = (opcode & bitmask);

		return instructions[ms][ls];
	}

	// ============================================
	// create instructions
	/**
	 * Initiate all the instructions and add to an array.
	 */
	private void getInstructionsSet() {
		instructions[0x06][0x09] = new Instruction("ADC", "immediate", InstructionSet.ADC(), InstructionSet.immediate(),
				2, 2);
		instructions[0x06][0x05] = new Instruction("ADC", "zeroPage", InstructionSet.ADC(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x07][0x05] = new Instruction("ADC", "zeroPageX", InstructionSet.ADC(), InstructionSet.zeroPageX(),
				4, 2);
		instructions[0x06][0x0D] = new Instruction("ADC", "absolute", InstructionSet.ADC(), InstructionSet.absolute(),
				4, 3);
		instructions[0x07][0x0D] = new Instruction("ADC", "absoluteX", InstructionSet.ADC(), InstructionSet.absoluteX(),
				4, 3);
		instructions[0x07][0x09] = new Instruction("ADC", "absoluteY", InstructionSet.ADC(), InstructionSet.absoluteY(),
				4, 3);
		instructions[0x06][0x01] = new Instruction("ADC", "indirectX", InstructionSet.ADC(), InstructionSet.indirectX(),
				6, 2);
		instructions[0x07][0x01] = new Instruction("ADC", "indirectY", InstructionSet.ADC(), InstructionSet.indirectY(),
				5, 2);
		instructions[0x02][0x09] = new Instruction("AND", "immediate", InstructionSet.AND(), InstructionSet.immediate(),
				2, 2);
		instructions[0x02][0x05] = new Instruction("AND", "zeroPage", InstructionSet.AND(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x03][0x05] = new Instruction("AND", "zeroPageX", InstructionSet.AND(), InstructionSet.zeroPageX(),
				4, 2);
		instructions[0x02][0x0D] = new Instruction("AND", "absolute", InstructionSet.AND(), InstructionSet.absolute(),
				4, 3);
		instructions[0x03][0x0D] = new Instruction("AND", "absoluteX", InstructionSet.AND(), InstructionSet.absoluteX(),
				4, 3);
		instructions[0x03][0x09] = new Instruction("AND", "absoluteY", InstructionSet.AND(), InstructionSet.absoluteY(),
				4, 3);
		instructions[0x02][0x01] = new Instruction("AND", "indirectX", InstructionSet.AND(), InstructionSet.indirectX(),
				6, 2);
		instructions[0x03][0x01] = new Instruction("AND", "indirectY", InstructionSet.AND(), InstructionSet.indirectY(),
				5, 2);
		instructions[0x00][0x0A] = new Instruction("ASL", "implied", InstructionSet.ASL(), InstructionSet.implied(), 2,
				1);
		instructions[0x00][0x06] = new Instruction("ASL", "zeroPage", InstructionSet.ASL(), InstructionSet.zeroPage(),
				5, 2);
		instructions[0x01][0x06] = new Instruction("ASL", "zeroPageX", InstructionSet.ASL(), InstructionSet.zeroPageX(),
				6, 2);
		instructions[0x00][0x0E] = new Instruction("ASL", "absolute", InstructionSet.ASL(), InstructionSet.absolute(),
				6, 3);
		instructions[0x01][0x0E] = new Instruction("ASL", "absoluteX", InstructionSet.ASL(), InstructionSet.absoluteX(),
				7, 3);
		instructions[0x09][0x00] = new Instruction("BCC", "relative", InstructionSet.BCC(), InstructionSet.relative(),
				2, 2);
		instructions[0x0B][0x00] = new Instruction("BCS", "relative", InstructionSet.BCS(), InstructionSet.relative(),
				2, 2);
		instructions[0x0F][0x00] = new Instruction("BEQ", "relative", InstructionSet.BEQ(), InstructionSet.relative(),
				2, 2);
		instructions[0x02][0x04] = new Instruction("BIT", "zeroPage", InstructionSet.BIT(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x02][0x0C] = new Instruction("BIT", "absolute", InstructionSet.BIT(), InstructionSet.absolute(),
				4, 3);
		instructions[0x03][0x00] = new Instruction("BMI", "relative", InstructionSet.BMI(), InstructionSet.relative(),
				2, 2);
		instructions[0x0D][0x00] = new Instruction("BNE", "relative", InstructionSet.BNE(), InstructionSet.relative(),
				2, 2);
		instructions[0x01][0x00] = new Instruction("BPL", "relative", InstructionSet.BPL(), InstructionSet.relative(),
				2, 2);
		instructions[0x00][0x00] = new Instruction("BRK", "implied", InstructionSet.BRK(), InstructionSet.implied(), 7,
				1);
		instructions[0x05][0x00] = new Instruction("BVC", "relative", InstructionSet.BVC(), InstructionSet.relative(),
				2, 2);
		instructions[0x07][0x00] = new Instruction("BVS", "relative", InstructionSet.BVS(), InstructionSet.relative(),
				2, 2);
		instructions[0x01][0x08] = new Instruction("CLC", "implied", InstructionSet.CLC(), InstructionSet.implied(), 2,
				1);
		instructions[0x0D][0x08] = new Instruction("CLD", "implied", InstructionSet.CLD(), InstructionSet.implied(), 2,
				1);
		instructions[0x05][0x08] = new Instruction("CLI", "implied", InstructionSet.CLI(), InstructionSet.implied(), 2,
				1);
		instructions[0x0B][0x08] = new Instruction("CLV", "implied", InstructionSet.CLV(), InstructionSet.implied(), 2,
				1);
		instructions[0x0C][0x09] = new Instruction("CMP", "immediate", InstructionSet.CMP(), InstructionSet.immediate(),
				2, 2);
		instructions[0x0C][0x05] = new Instruction("CMP", "zeroPage", InstructionSet.CMP(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x0D][0x05] = new Instruction("CMP", "zeroPageX", InstructionSet.CMP(), InstructionSet.zeroPageX(),
				4, 2);
		instructions[0x0C][0x0D] = new Instruction("CMP", "absolute", InstructionSet.CMP(), InstructionSet.absolute(),
				4, 3);
		instructions[0x0D][0x0D] = new Instruction("CMP", "absoluteX", InstructionSet.CMP(), InstructionSet.absoluteX(),
				4, 3);
		instructions[0x0D][0x09] = new Instruction("CMP", "absoluteY", InstructionSet.CMP(), InstructionSet.absoluteY(),
				4, 3);
		instructions[0x0C][0x01] = new Instruction("CMP", "indirectX", InstructionSet.CMP(), InstructionSet.indirectX(),
				6, 2);
		instructions[0x0D][0x01] = new Instruction("CMP", "indirectY", InstructionSet.CMP(), InstructionSet.indirectY(),
				5, 2);
		instructions[0x0E][0x00] = new Instruction("CPX", "immediate", InstructionSet.CPX(), InstructionSet.immediate(),
				2, 2);
		instructions[0x0E][0x04] = new Instruction("CPX", "zeroPage", InstructionSet.CPX(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x0E][0x0C] = new Instruction("CPX", "absolute", InstructionSet.CPX(), InstructionSet.absolute(),
				4, 3);
		instructions[0x0C][0x00] = new Instruction("CPY", "immediate", InstructionSet.CPY(), InstructionSet.immediate(),
				2, 2);
		instructions[0x0C][0x04] = new Instruction("CPY", "zeroPage", InstructionSet.CPY(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x0C][0x0C] = new Instruction("CPY", "absolute", InstructionSet.CPY(), InstructionSet.absolute(),
				4, 3);
		instructions[0x0C][0x06] = new Instruction("DEC", "zeroPage", InstructionSet.DEC(), InstructionSet.zeroPage(),
				5, 2);
		instructions[0x0D][0x06] = new Instruction("DEC", "zeroPageX", InstructionSet.DEC(), InstructionSet.zeroPageX(),
				6, 2);
		instructions[0x0C][0x0E] = new Instruction("DEC", "absolute", InstructionSet.DEC(), InstructionSet.absolute(),
				6, 3);
		instructions[0x0D][0x0E] = new Instruction("DEC", "absoluteX", InstructionSet.DEC(), InstructionSet.absoluteX(),
				7, 3);
		instructions[0x0C][0x0A] = new Instruction("DEX", "implied", InstructionSet.DEX(), InstructionSet.implied(), 2,
				1);
		instructions[0x08][0x08] = new Instruction("DEY", "implied", InstructionSet.DEY(), InstructionSet.implied(), 2,
				1);
		instructions[0x04][0x09] = new Instruction("EOR", "immediate", InstructionSet.EOR(), InstructionSet.immediate(),
				2, 2);
		instructions[0x04][0x05] = new Instruction("EOR", "zeroPage", InstructionSet.EOR(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x05][0x05] = new Instruction("EOR", "zeroPageX", InstructionSet.EOR(), InstructionSet.zeroPageX(),
				4, 2);
		instructions[0x04][0x0D] = new Instruction("EOR", "absolute", InstructionSet.EOR(), InstructionSet.absolute(),
				4, 3);
		instructions[0x05][0x0D] = new Instruction("EOR", "absoluteX", InstructionSet.EOR(), InstructionSet.absoluteX(),
				4, 3);
		instructions[0x05][0x09] = new Instruction("EOR", "absoluteY", InstructionSet.EOR(), InstructionSet.absoluteY(),
				4, 3);
		instructions[0x04][0x01] = new Instruction("EOR", "indirectX", InstructionSet.EOR(), InstructionSet.indirectX(),
				6, 2);
		instructions[0x05][0x01] = new Instruction("EOR", "indirectY", InstructionSet.EOR(), InstructionSet.indirectY(),
				5, 2);
		instructions[0x0E][0x06] = new Instruction("INC", "zeroPage", InstructionSet.INC(), InstructionSet.zeroPage(),
				5, 2);
		instructions[0x0F][0x06] = new Instruction("INC", "zeroPageX", InstructionSet.INC(), InstructionSet.zeroPageX(),
				6, 2);
		instructions[0x0E][0x0E] = new Instruction("INC", "absolute", InstructionSet.INC(), InstructionSet.absolute(),
				6, 3);
		instructions[0x0F][0x0E] = new Instruction("INC", "absoluteX", InstructionSet.INC(), InstructionSet.absoluteX(),
				7, 3);
		instructions[0x0E][0x08] = new Instruction("INX", "implied", InstructionSet.INX(), InstructionSet.implied(), 2,
				1);
		instructions[0x0C][0x08] = new Instruction("INY", "implied", InstructionSet.INY(), InstructionSet.implied(), 2,
				1);
		instructions[0x04][0x0C] = new Instruction("JMP", "absolute", InstructionSet.JMP(), InstructionSet.absolute(),
				3, 3);
		instructions[0x06][0x0C] = new Instruction("JMP", "indirect", InstructionSet.JMP(), InstructionSet.indirect(),
				5, 3);
		instructions[0x02][0x00] = new Instruction("JSR", "absolute", InstructionSet.JSR(), InstructionSet.absolute(),
				6, 3);
		instructions[0x0A][0x09] = new Instruction("LDA", "immediate", InstructionSet.LDA(), InstructionSet.immediate(),
				2, 2);
		instructions[0x0A][0x05] = new Instruction("LDA", "zeroPage", InstructionSet.LDA(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x0B][0x05] = new Instruction("LDA", "zeroPageX", InstructionSet.LDA(), InstructionSet.zeroPageX(),
				4, 2);
		instructions[0x0A][0x0D] = new Instruction("LDA", "absolute", InstructionSet.LDA(), InstructionSet.absolute(),
				4, 3);
		instructions[0x0B][0x0D] = new Instruction("LDA", "absoluteX", InstructionSet.LDA(), InstructionSet.absoluteX(),
				4, 3);
		instructions[0x0B][0x09] = new Instruction("LDA", "absoluteY", InstructionSet.LDA(), InstructionSet.absoluteY(),
				4, 3);
		instructions[0x0A][0x01] = new Instruction("LDA", "indirectX", InstructionSet.LDA(), InstructionSet.indirectX(),
				6, 2);
		instructions[0x0B][0x01] = new Instruction("LDA", "indirectY", InstructionSet.LDA(), InstructionSet.indirectY(),
				5, 2);
		instructions[0x0A][0x02] = new Instruction("LDX", "immediate", InstructionSet.LDX(), InstructionSet.immediate(),
				2, 2);
		instructions[0x0A][0x06] = new Instruction("LDX", "zeroPage", InstructionSet.LDX(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x0B][0x06] = new Instruction("LDX", "zeroPageY", InstructionSet.LDX(), InstructionSet.zeroPageY(),
				4, 2);
		instructions[0x0A][0x0E] = new Instruction("LDX", "absolute", InstructionSet.LDX(), InstructionSet.absolute(),
				4, 3);
		instructions[0x0B][0x0E] = new Instruction("LDX", "absoluteY", InstructionSet.LDX(), InstructionSet.absoluteY(),
				4, 3);
		instructions[0x0A][0x00] = new Instruction("LDY", "immediate", InstructionSet.LDY(), InstructionSet.immediate(),
				2, 2);
		instructions[0x0A][0x04] = new Instruction("LDY", "zeroPage", InstructionSet.LDY(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x0B][0x04] = new Instruction("LDY", "zeroPageX", InstructionSet.LDY(), InstructionSet.zeroPageX(),
				4, 2);
		instructions[0x0A][0x0C] = new Instruction("LDY", "absolute", InstructionSet.LDY(), InstructionSet.absolute(),
				4, 3);
		instructions[0x0B][0x0C] = new Instruction("LDY", "absoluteX", InstructionSet.LDY(), InstructionSet.absoluteX(),
				4, 3);
		instructions[0x04][0x0A] = new Instruction("LSR", "implied", InstructionSet.LSR(), InstructionSet.implied(), 2,
				1);
		instructions[0x04][0x06] = new Instruction("LSR", "zeroPage", InstructionSet.LSR(), InstructionSet.zeroPage(),
				5, 2);
		instructions[0x05][0x06] = new Instruction("LSR", "zeroPageX", InstructionSet.LSR(), InstructionSet.zeroPageX(),
				6, 2);
		instructions[0x04][0x0E] = new Instruction("LSR", "absolute", InstructionSet.LSR(), InstructionSet.absolute(),
				6, 3);
		instructions[0x05][0x0E] = new Instruction("LSR", "absoluteX", InstructionSet.LSR(), InstructionSet.absoluteX(),
				7, 3);
		instructions[0x0E][0x0A] = new Instruction("NOP", "implied", InstructionSet.NOP(), InstructionSet.implied(), 2,
				1);
		instructions[0x00][0x09] = new Instruction("ORA", "immediate", InstructionSet.ORA(), InstructionSet.immediate(),
				2, 2);
		instructions[0x00][0x05] = new Instruction("ORA", "zeroPage", InstructionSet.ORA(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x01][0x05] = new Instruction("ORA", "zeroPageX", InstructionSet.ORA(), InstructionSet.zeroPageX(),
				4, 2);
		instructions[0x00][0x0D] = new Instruction("ORA", "absolute", InstructionSet.ORA(), InstructionSet.absolute(),
				4, 3);
		instructions[0x01][0x0D] = new Instruction("ORA", "absoluteX", InstructionSet.ORA(), InstructionSet.absoluteX(),
				4, 3);
		instructions[0x01][0x09] = new Instruction("ORA", "absoluteY", InstructionSet.ORA(), InstructionSet.absoluteY(),
				4, 3);
		instructions[0x00][0x01] = new Instruction("ORA", "indirectX", InstructionSet.ORA(), InstructionSet.indirectX(),
				6, 2);
		instructions[0x01][0x01] = new Instruction("ORA", "indirectY", InstructionSet.ORA(), InstructionSet.indirectY(),
				5, 2);
		instructions[0x04][0x08] = new Instruction("PHA", "implied", InstructionSet.PHA(), InstructionSet.implied(), 3,
				1);
		instructions[0x00][0x08] = new Instruction("PHP", "implied", InstructionSet.PHP(), InstructionSet.implied(), 3,
				1);
		instructions[0x06][0x08] = new Instruction("PLA", "implied", InstructionSet.PLA(), InstructionSet.implied(), 4,
				1);
		instructions[0x02][0x08] = new Instruction("PLP", "implied", InstructionSet.PLP(), InstructionSet.implied(), 4,
				1);
		instructions[0x02][0x0A] = new Instruction("ROL", "implied", InstructionSet.ROL(), InstructionSet.implied(), 2,
				1);
		instructions[0x02][0x06] = new Instruction("ROL", "zeroPage", InstructionSet.ROL(), InstructionSet.zeroPage(),
				5, 2);
		instructions[0x03][0x06] = new Instruction("ROL", "zeroPageX", InstructionSet.ROL(), InstructionSet.zeroPageX(),
				6, 2);
		instructions[0x02][0x0E] = new Instruction("ROL", "absolute", InstructionSet.ROL(), InstructionSet.absolute(),
				6, 3);
		instructions[0x03][0x0E] = new Instruction("ROL", "absoluteX", InstructionSet.ROL(), InstructionSet.absoluteX(),
				7, 3);
		instructions[0x06][0x0A] = new Instruction("ROR", "implied", InstructionSet.ROR(), InstructionSet.implied(), 2,
				1);
		instructions[0x06][0x06] = new Instruction("ROR", "zeroPage", InstructionSet.ROR(), InstructionSet.zeroPage(),
				5, 2);
		instructions[0x07][0x06] = new Instruction("ROR", "zeroPageX", InstructionSet.ROR(), InstructionSet.zeroPageX(),
				6, 2);
		instructions[0x06][0x0E] = new Instruction("ROR", "absolute", InstructionSet.ROR(), InstructionSet.absolute(),
				6, 3);
		instructions[0x07][0x0E] = new Instruction("ROR", "absoluteX", InstructionSet.ROR(), InstructionSet.absoluteX(),
				7, 3);
		instructions[0x04][0x00] = new Instruction("RTI", "implied", InstructionSet.RTI(), InstructionSet.implied(), 6,
				1);
		instructions[0x06][0x00] = new Instruction("RTS", "implied", InstructionSet.RTS(), InstructionSet.implied(), 6,
				1);
		instructions[0x0E][0x09] = new Instruction("SBC", "immediate", InstructionSet.SBC(), InstructionSet.immediate(),
				2, 2);
		instructions[0x0E][0x05] = new Instruction("SBC", "zeroPage", InstructionSet.SBC(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x0F][0x05] = new Instruction("SBC", "zeroPageX", InstructionSet.SBC(), InstructionSet.zeroPageX(),
				4, 2);
		instructions[0x0E][0x0D] = new Instruction("SBC", "absolute", InstructionSet.SBC(), InstructionSet.absolute(),
				4, 3);
		instructions[0x0F][0x0D] = new Instruction("SBC", "absoluteX", InstructionSet.SBC(), InstructionSet.absoluteX(),
				4, 3);
		instructions[0x0F][0x09] = new Instruction("SBC", "absoluteY", InstructionSet.SBC(), InstructionSet.absoluteY(),
				4, 3);
		instructions[0x0E][0x01] = new Instruction("SBC", "indirectX", InstructionSet.SBC(), InstructionSet.indirectX(),
				6, 2);
		instructions[0x0F][0x01] = new Instruction("SBC", "indirectY", InstructionSet.SBC(), InstructionSet.indirectY(),
				5, 2);
		instructions[0x03][0x08] = new Instruction("SEC", "implied", InstructionSet.SEC(), InstructionSet.implied(), 2,
				1);
		instructions[0x0F][0x08] = new Instruction("SED", "implied", InstructionSet.SED(), InstructionSet.implied(), 2,
				1);
		instructions[0x07][0x08] = new Instruction("SEI", "implied", InstructionSet.SEI(), InstructionSet.implied(), 2,
				1);
		instructions[0x08][0x05] = new Instruction("STA", "zeroPage", InstructionSet.STA(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x09][0x05] = new Instruction("STA", "zeroPageX", InstructionSet.STA(), InstructionSet.zeroPageX(),
				4, 2);
		instructions[0x08][0x0D] = new Instruction("STA", "absolute", InstructionSet.STA(), InstructionSet.absolute(),
				4, 3);
		instructions[0x09][0x0D] = new Instruction("STA", "absoluteX", InstructionSet.STA(), InstructionSet.absoluteX(),
				5, 3);
		instructions[0x09][0x09] = new Instruction("STA", "absoluteY", InstructionSet.STA(), InstructionSet.absoluteY(),
				5, 3);
		instructions[0x08][0x01] = new Instruction("STA", "indirectX", InstructionSet.STA(), InstructionSet.indirectX(),
				6, 2);
		instructions[0x09][0x01] = new Instruction("STA", "indirectY", InstructionSet.STA(), InstructionSet.indirectY(),
				6, 2);
		instructions[0x08][0x06] = new Instruction("STX", "zeroPage", InstructionSet.STX(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x09][0x06] = new Instruction("STX", "zeroPageY", InstructionSet.STX(), InstructionSet.zeroPageY(),
				4, 2);
		instructions[0x08][0x0E] = new Instruction("STX", "absolute", InstructionSet.STX(), InstructionSet.absolute(),
				4, 3);
		instructions[0x08][0x04] = new Instruction("STY", "zeroPage", InstructionSet.STY(), InstructionSet.zeroPage(),
				3, 2);
		instructions[0x09][0x04] = new Instruction("STY", "zeroPageX", InstructionSet.STY(), InstructionSet.zeroPageX(),
				4, 2);
		instructions[0x08][0x0C] = new Instruction("STY", "absolute", InstructionSet.STY(), InstructionSet.absolute(),
				4, 3);
		instructions[0x0A][0x0A] = new Instruction("TAX", "implied", InstructionSet.TAX(), InstructionSet.implied(), 2,
				1);
		instructions[0x0A][0x08] = new Instruction("TAY", "implied", InstructionSet.TAY(), InstructionSet.implied(), 2,
				1);
		instructions[0x0B][0x0A] = new Instruction("TSX", "implied", InstructionSet.TSX(), InstructionSet.implied(), 2,
				1);
		instructions[0x08][0x0A] = new Instruction("TXA", "implied", InstructionSet.TXA(), InstructionSet.implied(), 2,
				1);
		instructions[0x09][0x0A] = new Instruction("TXS", "implied", InstructionSet.TXS(), InstructionSet.implied(), 2,
				1);
		instructions[0x09][0x08] = new Instruction("TYA", "implied", InstructionSet.TYA(), InstructionSet.implied(), 2,
				1);
	}

	// = = = = = = = = = = = = = = = = = = = = = = = = = = = = == = =
	// Test Client
	// = = = = = = = = = = = = = = = = = = = = = = = = = = = = == = =

	public static void main(String[] arg) {

	}
}