/*
 * TODO: A place to keep all the notes, references, and considerations in one place. Consider 
 * which notes will be discarded and which will make it to the finalized project.
 * 
 * REFERENCE AND INSPIRATION:
 * http://www.emulator101.com/6502-addressing-modes.html - 
 * https://www.youtube.com/watch?v=8XmxKPJDGU0&t=2222s - one lone coder
 * https://skilldrick.github.io/easy6502/#addressing - easy6502 (tutorial)
 * http://www.obelisk.me.uk/6502/reference.html#BRK - 6502 reference
 * https://www.youtube.com/watch?v=yl8vPW5hydQ
 * 
 * NOTES: 
 * *****
 * Page crossing means the high byte of an address doesn't match the high byte of another. Some instructions 
 * require an extra cycle when page is crossed. i.e., (addr << 0xff00) & (addr2 << 0xff00)   
 * 
 * 
 */

/*
 * 
 * 
 */
package cpu;

import helpers.Hexdump;

public class InstructionSet {

	// ==========================================
	// All the instructions

	/**
	 * Add memory to accummulator with carry.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> ADC() {
		return (cpu) -> {

			// Get the value in memory, the accumulator and add them along with the carry
			// bit.
			int m = cpu.read(cpu.handlingData);
			int a = cpu.getA();
			int c = (cpu.getFlag(Flag.CARRY)) ? 1 : 0;
			int result = a + m + c;

			if (result > 255)
				cpu.setFlag(Flag.CARRY);
			else
				cpu.clearFlag(Flag.CARRY);

			if ((result & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			// ~(A^M) & (A^R)
			if ((((result ^ a) & (result ^ m)) & 0x0080) == 0x80)
				cpu.setFlag(Flag.OVERFLOW);
			else
				cpu.clearFlag(Flag.OVERFLOW);

			if ((result & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			// load the first eight bits of the result.
			cpu.setA(result & 0xff);

			// Return 1 for the extra cycles.
			return 1;
		};
	}

	/**
	 * Shift all bits left one bit, Memory or Accumulator.<br>
	 * A = C <- (A << 1) <- 0<br>
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> ASL() {
		return (cpu) -> {
			int t = fetchData(cpu);

			t = t << 1;

			if ((t & 0xff00) > 0)
				cpu.setFlag(Flag.CARRY);
			else
				cpu.clearFlag(Flag.CARRY);

			if ((t & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((t & 0x80) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			if (cpu.getCurrentInstruction().modeName == "implied")
				cpu.setA(t & 0x00ff); // Set it and throw away the high byte.
			else
				cpu.write(cpu.handlingData, t & 0x00ff);// Set it and throw away the high byte.

			return 0;
		};
	}

	/**
	 * "AND" memory with accumulator.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> AND() {
		return (cpu) -> {
			int value = cpu.read(cpu.handlingData);

			cpu.setA(cpu.getA() & value);

			if (cpu.getA() == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((cpu.getA() & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 1;
		};
	}

	/**
	 * BCC - Branch if Carry Clear<br>
	 * If the carry flag is clear then add the relative displacement to the program
	 * counter to cause a branch to a new location.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> BCC() {
		return (cpu) -> {
			if (!cpu.getFlag(Flag.CARRY)) {
				cpu.cycle++;
				int t = cpu.getProgramCounter() + cpu.handlingData;

				if ((cpu.handlingData & 0xff00) != (cpu.getProgramCounter() & 0xff00))
					cpu.cycle++;

				cpu.setProgramCounter(t);
			}
			return 0;
		};
	}

	/**
	 * BCS - Branch if Carry Set<br>
	 * If the carry flag is set then add the relative displacement to the program
	 * counter to cause a branch to a new location.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> BCS() {
		return (cpu) -> {
			if (cpu.getFlag(Flag.CARRY)) {
				cpu.cycle++;
				int t = cpu.getProgramCounter() + cpu.handlingData;

				if ((cpu.handlingData & 0xff00) != (cpu.getProgramCounter() & 0xff00))
					cpu.cycle++;

				cpu.setProgramCounter(t);
			}
			return 0;
		};
	}

	/**
	 * BEQ - Branch if Equal<br>
	 * If the zero flag is set then add the relative displacement to the program
	 * counter to cause a branch to a new location.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> BEQ() {
		return (cpu) -> {
			if (cpu.getFlag(Flag.ZERO)) {
				cpu.cycle++;
				int t = cpu.getProgramCounter() + cpu.handlingData;

				if ((cpu.handlingData & 0xff00) != (cpu.getProgramCounter() & 0xff00))
					cpu.cycle++;

				cpu.setProgramCounter(t);
			}
			return 0;
		};
	}

	/**
	 * BIT - Bit Test <br>
	 * A & M, N = M7, V = M6<br>
	 * 
	 * This instructions is used to test if one or more bits are set in a target
	 * memory location. The mask pattern in A is ANDed with the value in memory to
	 * set or clear the zero flag, but the result is not kept. Bits 7 and 6 of the
	 * value from memory are copied into the N and V flags.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> BIT() {
		return (cpu) -> {
			int a = cpu.getA();
			int m = cpu.read(cpu.handlingData);

			int result = a & m;

			if ((result & 0x00ff) == 0x00)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((m & (1 << 6)) == 0)
				cpu.clearFlag(Flag.OVERFLOW);
			else
				cpu.setFlag(Flag.OVERFLOW);

			if ((m & (1 << 7)) == 0)
				cpu.clearFlag(Flag.NEGATIVE);
			else
				cpu.setFlag(Flag.NEGATIVE);

			return 0;
		};
	}

	/**
	 * BMI - Branch if Minus<br>
	 * If the negative flag is set then add the relative displacement to the program
	 * counter to cause a branch to a new location.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> BMI() {
		return (cpu) -> {
			if (cpu.getFlag(Flag.NEGATIVE)) {
				cpu.cycle++;
				int t = cpu.getProgramCounter() + cpu.handlingData;

				if ((cpu.handlingData & 0xff00) != (cpu.getProgramCounter() & 0xff00))
					cpu.cycle++;

				cpu.setProgramCounter(t);
			}
			return 0;
		};
	}

	/**
	 * BNE - Branch if Not Equal<br>
	 * If the zero flag is clear then add the relative displacement to the program
	 * counter to cause a branch to a new location.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> BNE() {
		return (cpu) -> {
			if (!cpu.getFlag(Flag.ZERO)) {
				cpu.cycle++;
				int t = cpu.getProgramCounter() + cpu.handlingData;

				if ((cpu.handlingData & 0xff00) != (cpu.getProgramCounter() & 0xff00))
					cpu.cycle++;

				cpu.setProgramCounter(t);
			}
			return 0;
		};
	}

	/**
	 * BPL - Branch if Positive<br>
	 * If the negative flag is clear then add the relative displacement to the
	 * program counter to cause a branch to a new location.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> BPL() {
		return (cpu) -> {
			if (!cpu.getFlag(Flag.NEGATIVE)) {
				cpu.cycle++;
				int t = cpu.getProgramCounter() + cpu.handlingData;

				if ((cpu.handlingData & 0xff00) != (cpu.getProgramCounter() & 0xff00))
					cpu.cycle++;

				cpu.setProgramCounter(t);
			}
			return 0;
		};
	}

	/**
	 * BRK - Force Interrupt<br>
	 * The BRK instruction forces the generation of an interrupt request. The
	 * program counter and processor status are pushed on the stack then the IRQ
	 * interrupt vector at $FFFE/F is loaded into the PC and the break flag in the
	 * status set to one.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> BRK() {
		return (cpu) -> {
			cpu.incPC();

			cpu.setFlag(Flag.INTERRUPT_DISABLE);

			cpu.write(0x100 + cpu.getStackPointer(), cpu.getProgramCounter() & 0xff00);
			cpu.decSP(); // decreases the stack pointer.

			cpu.write(0x100 + cpu.getStackPointer(), cpu.getProgramCounter() & 0x00ff);
			cpu.decSP(); // decreases the stack pointer.

			cpu.setFlag(Flag.BREAK);
			cpu.write(0x100 + cpu.getStackPointer(), cpu.getStatus());
			cpu.decSP(); // decreases the stack pointer.

			cpu.clearFlag(Flag.BREAK);

			cpu.setProgramCounter(cpu.read(0xfffe) | (cpu.read(0xffff) << 8));
			return 0;
		};
	}

	/**
	 * BVC - Branch if Overflow Clear<br>
	 * If the overflow flag is clear then add the relative displacement to the
	 * program counter to cause a branch to a new location.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> BVC() {
		return (cpu) -> {
			if (!cpu.getFlag(Flag.OVERFLOW)) {
				cpu.cycle++;
				int t = cpu.getProgramCounter() + cpu.handlingData;

				if ((cpu.handlingData & 0xff00) != (cpu.getProgramCounter() & 0xff00))
					cpu.cycle++;

				cpu.setProgramCounter(t);
			}
			return 0;
		};
	}

	/**
	 * BVS - Branch if Overflow Set<br>
	 * If the overflow flag is set then add the relative displacement to the program
	 * counter to cause a branch to a new location.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> BVS() {
		return (cpu) -> {
			if (cpu.getFlag(Flag.OVERFLOW)) {
				cpu.cycle++;
				int t = cpu.getProgramCounter() + cpu.handlingData;

				if ((cpu.handlingData & 0xff00) != (cpu.getProgramCounter() & 0xff00))
					cpu.cycle++;

				cpu.setProgramCounter(t);
			}
			return 0;
		};
	}

	/**
	 * CLC - Clear Carry Flag<br>
	 * C = 0 <br>
	 * 
	 * Set the carry flag to zero.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> CLC() {
		return (cpu) -> {
			cpu.clearFlag(Flag.CARRY);
			return 0;
		};
	}

	/**
	 * CLD - Clear Decimal Mode<br>
	 * D = 0<br>
	 * 
	 * Sets the decimal mode flag to zero.<br>
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> CLD() {
		return (cpu) -> {
			cpu.clearFlag(Flag.DECIMAL_MODE);
			return 0;
		};
	}

	/**
	 * CLI - Clear Interrupt Disable<br>
	 * I = 0<br>
	 * 
	 * Clears the interrupt disable flag allowing normal interrupt requests to be
	 * service
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> CLI() {
		return (cpu) -> {
			cpu.clearFlag(Flag.INTERRUPT_DISABLE);
			return 0;
		};
	}

	/**
	 * CLV - Clear Overflow Flag<br>
	 * V = 0<br>
	 * 
	 * Clears the overflow flag.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> CLV() {
		return (cpu) -> {
			cpu.clearFlag(Flag.OVERFLOW);
			return 0;
		};
	}

	/**
	 * CMP - Compare<br>
	 * Z,C,N = A-M<br>
	 * 
	 * This instruction compares the contents of the accumulator with another memory
	 * held value and sets the zero and carry flags as appropriate.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> CMP() {
		return (cpu) -> {
			int cmp = cpu.getA() - cpu.read(cpu.handlingData);

			if (cpu.getA() >= cpu.read(cpu.handlingData))
				cpu.setFlag(Flag.CARRY);
			else
				cpu.clearFlag(Flag.CARRY);

			if ((cmp & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((cmp & 0x0080) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 1;
		};
	}

	/**
	 * CPX - Compare X Register<br>
	 * Z,C,N = X-M<br>
	 * <br>
	 * 
	 * This instruction compares the contents of the X register with another memory
	 * held value and sets the zero and carry flags as appropriate.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> CPX() {
		return (cpu) -> {

			int cmp = cpu.getX() - cpu.read(cpu.handlingData);

			if (cpu.getX() >= cpu.read(cpu.handlingData))
				cpu.setFlag(Flag.CARRY);
			else
				cpu.clearFlag(Flag.CARRY);

			if ((cmp & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((cmp & 0x0080) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 0;
		};
	}

	/**
	 * CPY - Compare Y Register<br>
	 * Z,C,N = Y-M<br>
	 * 
	 * This instruction compares the contents of the Y register with another memory
	 * held value and sets the zero and carry flags as appropriate.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> CPY() {
		return (cpu) -> {
			int m = cpu.read(cpu.handlingData);
			int cmp = cpu.getY() - m;

			if (cpu.getY() >= m)
				cpu.setFlag(Flag.CARRY);
			else
				cpu.clearFlag(Flag.CARRY);

			if ((cmp & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((cmp & 0x80) == 0x80)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 0;
		};
	}

	/**
	 * DEC - Decrement Memory<br>
	 * M,Z,N = M-1<br>
	 * 
	 * Subtracts one from the value held at a specified memory location setting the
	 * zero and negative flags as appropriate.
	 * 
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> DEC() {
		return (cpu) -> {
			int t = cpu.read(cpu.handlingData) - 1;
			t &= 0x00ff;

			cpu.write(cpu.handlingData, t);

			if ((t & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((t & 0x80) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 0;
		};
	}

	/**
	 * DEX - Decrement X Register<br>
	 * X,Z,N = X-1<br>
	 * 
	 * Subtracts one from the X register setting the zero and negative flags as
	 * appropriate.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> DEX() {
		return (cpu) -> {

			int t = cpu.getX() - 1;
			t &= 0x00ff;

			if (t == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((t & 0x80) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			cpu.setX(t);
			return 0;
		};
	}

	/**
	 * DEY - Decrement Y Register<br>
	 * Y,Z,N = Y-1<br>
	 * 
	 * Subtracts one from the Y register setting the zero and negative flags as
	 * appropriate.
	 * 
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> DEY() {
		return (cpu) -> {

			int t = cpu.getY() - 1;
			t &= 0x00ff;

			if ((t & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((t & 0x80) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			cpu.setY(t);

			return 0;
		};
	}

	/**
	 * EOR - Exclusive OR<br>
	 * A,Z,N = A^M<br>
	 * 
	 * An exclusive OR is performed, bit by bit, on the accumulator contents using
	 * the contents of a byte of memory.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> EOR() {
		return (cpu) -> {
			int value = cpu.read(cpu.handlingData);
			int t = cpu.getA() ^ value;

			if ((t & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((t & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			cpu.setA(t);

			return 1;
		};
	}

	/**
	 * INC - Increment Memory<br>
	 * M,Z,N = M+1<br>
	 * 
	 * Adds one to the value held at a specified memory location setting the zero
	 * and negative flags as appropriate.
	 * 
	 * @return
	 */
	public static Executable<CPU6502> INC() {
		return (cpu) -> {
			int t = cpu.read(cpu.handlingData);

			t++;
			t = t % 256;

			if ((t & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((t & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			cpu.write(cpu.handlingData, t);

			return 0;
		};
	}

	/**
	 * adds one to the X register setting the zero and negative flags as
	 * appropriate.
	 * 
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> INX() {
		return (cpu) -> {

			int t = cpu.getX() + 1;

			cpu.setX(t);

			if ((t & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			// 01001010
			if ((t & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 0;

		};
	}

	/**
	 * 
	 * adds one to the Y register setting the zero and negative flags as
	 * appropriate.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> INY() {
		return (cpu) -> {
			int t = cpu.getY() + 1;

			cpu.setY(t);
			if ((t & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			// 01001010
			if ((t & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 0;
		};
	}

	/**
	 * JMP - Jump<br>
	 * Sets the program counter to the address specified by the operand.
	 * 
	 * @return
	 */
	public static Executable<CPU6502> JMP() {
		return (cpu) -> {
			cpu.setProgramCounter(cpu.handlingData);
			cpu.decPC();
			return 0;
		};
	}

	/**
	 * JSR - Jump to Subroutine<br>
	 * The JSR instruction pushes the address (minus one) of the return point on to
	 * the stack and then sets the program counter to the target memory address.
	 * 
	 * @return
	 */
	public static Executable<CPU6502> JSR() {
		return (cpu) -> {
//			cpu.decPC();

			cpu.write(0x100 + cpu.getStackPointer(), (cpu.getProgramCounter() >> 8));
			cpu.decSP(); // decreases the stack pointer.

			cpu.write(0x100 + cpu.getStackPointer(), cpu.getProgramCounter() & 0x00ff);
			cpu.decSP(); // decreases the stack pointer.

			cpu.setProgramCounter(--cpu.handlingData);
			return 0;
		};
	}

	/**
	 * LDA - Load Accumulator A,Z,N = M
	 * 
	 * Loads a byte of memory into the accumulator setting the zero and negative
	 * flags as appropriate.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> LDA() {
		return (cpu) -> {
			cpu.setA(cpu.read(cpu.handlingData));

			if (cpu.getA() == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			// 01001010
			if ((cpu.getA() & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 1;
		};
	}

	/**
	 * Loads a byte of memory into the X register setting the zero and negative
	 * flags as appropriate.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> LDX() {
		return (cpu) -> {
			cpu.setX(cpu.read(cpu.handlingData));

			if (cpu.getX() == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			// 01001010
			if ((cpu.getX() & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 1;
		};
	}

	/**
	 * Loads a byte of memory into the Y register setting the zero and negative
	 * flags as appropriate.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> LDY() {
		return (cpu) -> {
			cpu.setY(cpu.read(cpu.handlingData));

			if (cpu.getY() == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			// 01001010
			if ((cpu.getY() & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 1;

		};
	}

	/**
	 * LSR - Logical Shift Right<br>
	 * A,C,Z,N = A/2 or M,C,Z,N = M/2<br>
	 * 
	 * Each of the bits in A or M is shift one place to the right. The bit that was
	 * in bit 0 is shifted into the carry flag. Bit 7 is set to zero.
	 * 
	 * @return
	 */
	public static Executable<CPU6502> LSR() {
		return (cpu) -> {
			int t = fetchData(cpu);

			if ((t & 0b0000_0001) == 1)
				cpu.setFlag(Flag.CARRY);
			else
				cpu.clearFlag(Flag.CARRY);

			t = t >> 1;

			if ((t & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((t & 0x80) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			if (cpu.getCurrentInstruction().modeName == "implied")
				cpu.setA(t & 0x00ff); // Set it and throw away the high bytes.
			else
				cpu.write(cpu.handlingData, t & 0x00ff);

			return 0;
		};
	}

	/**
	 * NOP - No Operation<br>
	 * The NOP instruction causes no changes to the processor other than the normal
	 * incrementing of the program counter to the next instruction.
	 * 
	 * @return
	 */
	public static Executable<CPU6502> NOP() {
		return (cpu) -> {
			// cpu.incPC();
			return 0;
		};
	}

	/**
	 * ORA - Logical Inclusive OR<br>
	 * A,Z,N = A|M<br>
	 * 
	 * An inclusive OR is performed, bit by bit, on the accumulator contents using
	 * the contents of a byte of memory.
	 * 
	 * @return
	 */
	public static Executable<CPU6502> ORA() {
		return (cpu) -> {
			int value = cpu.read(cpu.handlingData);

			cpu.setA((cpu.getA() | value) & 0x00ff);

			if (cpu.getA() == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((cpu.getA() & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 1;
		};
	}

	/**
	 * PHA - Push Accumulato<br>
	 * Pushes a copy of the accumulator on to the stack.
	 * 
	 * @return
	 */
	public static Executable<CPU6502> PHA() {
		return (cpu) -> {
			int a = cpu.getA();

			cpu.write(0x100 + cpu.getStackPointer(), a);
			cpu.decSP();

			return 0;
		};
	}

	/**
	 * PHP - Push Processor Status<br>
	 * Pushes a copy of the status flags on to the stack.
	 * 
	 * @return
	 */
	public static Executable<CPU6502> PHP() {
		return (cpu) -> {
			cpu.setFlag(Flag.BREAK);
			cpu.setFlag(Flag.UNUSED);

			cpu.write(0x100 + cpu.getStackPointer(), cpu.getStatus());

			cpu.clearFlag(Flag.BREAK);
			cpu.clearFlag(Flag.UNUSED);
			cpu.decSP();
			return 0;
		};
	}

	/**
	 * PLA - Pull Accumulator<br>
	 * Pulls an 8 bit value from the stack and into the accumulator. The zero and
	 * negative flags are set as appropriate.
	 * 
	 * @return
	 */
	public static Executable<CPU6502> PLA() {
		return (cpu) -> {
			cpu.incSP();

			cpu.setA(cpu.read(0x100 + cpu.getStackPointer()));

			if (cpu.getA() == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((cpu.getA() & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 0;
		};
	}

	/**
	 * PLP - Pull Processor Status<br>
	 * Pulls an 8 bit value from the stack and into the processor flags. The flags
	 * will take on new states as determined by the value pulled.
	 * 
	 * @return
	 */
	public static Executable<CPU6502> PLP() {
		return (cpu) -> {
			cpu.incSP();
			int newStatus = cpu.read(0x100 + cpu.getStackPointer());
			cpu.setStatus(newStatus);
			cpu.setFlag(Flag.UNUSED);

			return 0;
		};
	}

	/**
	 * ROL - Rotate Left<br>
	 * Move each of the bits in either A or M one place to the left. Bit 0 is filled
	 * with the current value of the carry flag whilst the old bit 7 becomes the new
	 * carry flag value.
	 * 
	 * @return
	 */
	public static Executable<CPU6502> ROL() {
		return (cpu) -> {
			int t = fetchData(cpu);

			t = t << 1;
			t |= cpu.getFlag(Flag.CARRY) ? 1 : 0;

			if ((t & 0xff00) > 0)
				cpu.setFlag(Flag.CARRY);
			else
				cpu.clearFlag(Flag.CARRY);

			if ((t & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((t & 0x80) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			if (cpu.getCurrentInstruction().modeName == "implied")
				cpu.setA(t & 0x00ff); // Set it and throw away the high bytes.
			else
				cpu.write(cpu.handlingData, t & 0x00ff);

			return 0;
		};
	}

	/**
	 * ROR - Rotate Right<br>
	 * Move each of the bits in either A or M one place to the right. Bit 7 is
	 * filled with the current value of the carry flag whilst the old bit 0 becomes
	 * the new carry flag value.
	 * 
	 * @return
	 */
	public static Executable<CPU6502> ROR() {
		return (cpu) -> {
			int t = fetchData(cpu);
			int c = cpu.getFlag(Flag.CARRY) ? 1 : 0;

			if ((t & 0b0000_0001) == 1)
				cpu.setFlag(Flag.CARRY);
			else
				cpu.clearFlag(Flag.CARRY);

			t = t >> 1;
			if (c == 1)
				t |= 1 << 7;
			else
				t &= ~(1 << 7);
//			t |= cpu.getFlag(Flag.CARRY) ? 1 << 7 : 0;

			if ((t & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			if ((t & 0x80) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			if (cpu.getCurrentInstruction().modeName == "implied")
				cpu.setA(t & 0x00ff); // Set it and throw away the high bytes.
			else
				cpu.write(cpu.handlingData, t & 0x00ff);

			return 0;
		};
	}

	/**
	 * Used for opcodes that have no instructions associated with it.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> DNE() {
		return (cpu) -> {
			return 0;
		};
	}

	/**
	 * RTI - Return from Interrupt<br>
	 * The RTI instruction is used at the end of an interrupt processing routine. It
	 * pulls the processor flags from the stack followed by the program counter.
	 * 
	 * @return
	 */
	public static Executable<CPU6502> RTI() {
		return (cpu) -> {

			// Get the Processor flags.
			cpu.incSP();
			cpu.setStatus(cpu.read(0x0100 + cpu.getStackPointer()));
			cpu.setStatus(cpu.getStatus() & ~Flag.BREAK.getFlag());
			cpu.setStatus(cpu.getStatus() & ~Flag.UNUSED.getFlag());

			cpu.incSP();
			int lowByte = cpu.read(0x0100 + cpu.getStackPointer());

			cpu.incSP();
			int highByte = cpu.read(0x0100 + cpu.getStackPointer());

			cpu.setProgramCounter((highByte << 8) | lowByte);
			cpu.decPC();

			Hexdump.printHexPadded(cpu.getProgramCounter(), 4);

			return 0;
		};
	}

	/**
	 * RTS - Return from Subroutine<br>
	 * The RTS instruction is used at the end of a subroutine to return to the
	 * calling routine. It pulls the program counter (minus one) from the stack.
	 * 
	 * 
	 * @return
	 */
	public static Executable<CPU6502> RTS() {
		return (cpu) -> {
//            cpu.incSP();

			cpu.incSP();
			int lowByte = cpu.read(0x0100 + cpu.getStackPointer());

			cpu.incSP();
			int highByte = cpu.read(0x0100 + cpu.getStackPointer());

			cpu.setProgramCounter((highByte << 8) | lowByte);
//			cpu.incPC();
			return 0;
		};
	}

	/**
	 * Subtract memory from accumulator with borrow.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> SBC() {
		return (cpu) -> {

			// Get the value in memory, the accumulator
			int m = cpu.read(cpu.handlingData);

			// invert the lower byte.
			m ^= 0x00ff;

			int a = cpu.getA();
			int c = (cpu.getFlag(Flag.CARRY)) ? 1 : 0;
			int temp = a + m + c;

			if (temp > 255)
				cpu.setFlag(Flag.CARRY);
			else
				cpu.clearFlag(Flag.CARRY);

			if ((temp & 0x00ff) == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			// ~(A^M) & (A^R)
			if ((((temp ^ a) & (temp ^ m)) & 0x0080) == 0x80)
				cpu.setFlag(Flag.OVERFLOW);
			else
				cpu.clearFlag(Flag.OVERFLOW);

			if ((temp & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			// load the first eight bits of the result.
			cpu.setA(temp & 0xff);

			// Return 1 for the extra cycles.
			return 1;
		};
	}

	/**
	 * SEC - Set Carry Flag<br>
	 * C = 1<br>
	 * 
	 * @return
	 */
	public static Executable<CPU6502> SEC() {
		return (cpu) -> {
			cpu.setFlag(Flag.CARRY);
			return 0;
		};
	}

	/**
	 * SED - Set Decimal Flag<br>
	 * D = 1<br>
	 * 
	 * @return
	 */
	public static Executable<CPU6502> SED() {
		return (cpu) -> {
			cpu.setFlag(Flag.DECIMAL_MODE);
			return 0;
		};
	}

	/**
	 * SEI - Set Interrupt Disable.<br>
	 * <code>I</code> = 1<br>
	 * 
	 * @return
	 */
	public static Executable<CPU6502> SEI() {
		return (cpu) -> {
			cpu.setFlag(Flag.INTERRUPT_DISABLE);
			return 0;
		};
	}

	/**
	 * Store accumulator in memory.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */

	public static Executable<CPU6502> STA() {
		return (cpu) -> {
			cpu.write(cpu.handlingData, cpu.getA());
			return 0;
		};
	}

	/**
	 * Store X into memory.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> STX() {
		return (cpu) -> {
			cpu.write(cpu.handlingData, cpu.getX());
			return 0;
		};
	}

	/**
	 * Store Y into memory.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> STY() {
		return (cpu) -> {
			cpu.write(cpu.handlingData, cpu.getY());
			return 0;
		};
	}

	/**
	 * Transfer accumulator to x. set flag Z and N
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> TAX() {
		return (cpu) -> {
			cpu.setX(cpu.getA());
			if (cpu.getX() == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			// 01001010
			if ((cpu.getX() & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 0;
		};
	}

	/**
	 * Transfer accumulator to Y
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> TAY() {
		return (cpu) -> {
			cpu.setY(cpu.getA());
			if (cpu.getY() == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			// 01001010
			if ((cpu.getY() & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 0;
		};
	}

	/**
	 * TSX - Transfer Stack Pointer to X<br>
	 * X = S<br>
	 * 
	 * Copies the current contents of the stack register into the X register and
	 * sets the zero and negative flags as appropriate.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> TSX() {
		return (cpu) -> {
			cpu.setX(cpu.getStackPointer());

			if (cpu.getX() == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			// 01001010
			if ((cpu.getX() & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 0;
		};
	}

	/**
	 * TXA - Transfer X to Accumulator<br>
	 * A = X<br>
	 * 
	 * Copies the current contents of the X register into the accumulator and sets
	 * the zero and negative flags as appropriate.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> TXA() {
		return (cpu) -> {
			cpu.setA(cpu.getX());

			if (cpu.getA() == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			// 01001010
			if ((cpu.getA() & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 0;
		};
	}

	/**
	 * Transfer x to the stack pointer.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> TXS() {
		return (cpu) -> {
			cpu.setStackPointer(cpu.getX());
			return 0;
		};
	}

	/**
	 * Transfer y to accumulator.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> TYA() {
		return (cpu) -> {
//			cpu.setY(cpu.getA());

			cpu.setA(cpu.getY());
			if (cpu.getA() == 0)
				cpu.setFlag(Flag.ZERO);
			else
				cpu.clearFlag(Flag.ZERO);

			// 01001010
			if ((cpu.getA() & (1 << 7)) != 0)
				cpu.setFlag(Flag.NEGATIVE);
			else
				cpu.clearFlag(Flag.NEGATIVE);

			return 0;
		};
	}

	// ==============================

	// All the instruction modes.
	// The methods return an Executable<CPU_6502>.

	/**
	 * The immediate mode will take the next byte as the value.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> immediate() {
		return (cpu) -> {
			cpu.incPC();
			cpu.handlingData = cpu.getProgramCounter();
			return 0;
		};
	}

	/**
	 * The source and destination of the information to be manipulated is implied
	 * directly by the function of the instruction itself and no further operand
	 * needs to be specified.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> implied() {
		return (cpu) -> {
			cpu.handlingData = cpu.getA();
			return 0;
		};
	}

	/**
	 * <p>
	 * The zero page instructions allow for shorter code execution by fetching only
	 * the second byte of the instruction and assuming a zero high address byte.
	 * </p>
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> zeroPage() {
		return (cpu) -> {
			cpu.incPC();
			cpu.handlingData = cpu.read(cpu.getProgramCounter());
			cpu.handlingData &= 0x00ff;

			return 0;
		};
	}

	/**
	 * Zero page indexed addressing. The effective address is calculated by adding
	 * the second byte to the contents of the index register. Since this is a form
	 * of "Zero Page" addressing, the content of the second byte references a
	 * location in page zero. no carry is added to the high order eight bits of
	 * memory and crossing of page boundaries does not occur.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> zeroPageX() {
		return (cpu) -> {
			cpu.incPC();
			cpu.handlingData = cpu.read(cpu.getProgramCounter());
			cpu.handlingData += cpu.getX();
			cpu.handlingData &= 0x00ff;

			return 0;
		};
	}

	/**
	 * The address to be accessed by an instruction using indexed zero page
	 * addressing is calculated by taking the 8 bit zero page address from the
	 * instruction and adding the current value of the Y register to it. This mode
	 * can only be used with the LDX and STX instructions.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> zeroPageY() {
		return (cpu) -> {
			cpu.incPC();
			cpu.handlingData = cpu.read(cpu.getProgramCounter());
			cpu.handlingData += cpu.getY();
			cpu.handlingData &= 0x00ff;

			return 0;
		};
	}

	/**
	 * Relative addressing is used only with branch instructions and establishes a
	 * destination for the conditional branch. The second byte of the instruction
	 * becomes the operand which is an "Offset" added to the contents of the lower
	 * eight bits of the program counter when the counter is set at the next
	 * instruction. The range of the offset is -128 to + 127 byte from the next
	 * instruction.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> relative() {
		return (cpu) -> {
			cpu.incPC();
			int offset = cpu.read(cpu.getProgramCounter());

			int temp = ~(offset - 1) & 0xff;
			if ((offset & 0x80) != 0) {
				offset = temp * -1;
			}
//			if ((offset & (0x80)) != 0)
//				offset |= 0xff00;
			cpu.handlingData = offset;

			return 0;
		};
	}

	/**
	 * Absolute addressing - In absolute addressing, the second byte of the
	 * instruction specifies the eight low order bits of the effective address while
	 * the third byte specifies the eight high order bits. Thus the absolute
	 * addressing mode allows access to the entire 64k bytes of addressable memory.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> absolute() {
		return (cpu) -> {
			cpu.incPC();
			int lowBytes = cpu.read(cpu.getProgramCounter());
			int highBytes = cpu.read(cpu.getProgramCounter() + 1);
			cpu.handlingData = (highBytes << 8) | lowBytes;

			cpu.incPC();
			return 0;
		};
	}

	/**
	 * This form of addressing is used in conjunction with X index register and is
	 * referred to as "Absolute, X". The effective address is formed by adding the
	 * contents of X to the address contained in the second and third bytes of the
	 * instruction. This mode allows the index register to contain the index or
	 * count value and instruction to contain the base address. This type of
	 * indexing allows any location referencing and thin index to modify fields,
	 * resulting in reduced coding and execution time.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> absoluteX() {
		return (cpu) -> {

			cpu.incPC();
			int lowBytes = cpu.read(cpu.getProgramCounter());
			int highBytes = cpu.read(cpu.getProgramCounter() + 1);
			int tempAddrs = (highBytes << 8) | lowBytes;
			cpu.incPC();

			cpu.setAbsoluteAddress(tempAddrs + cpu.getX());
			if ((cpu.handlingData & 0xff00) != (highBytes << 8))
				return 1;

			return 0;
		};
	}

	/**
	 * This form of addressing is used in conjunction with Y index register and is
	 * referred to as "Absolute, Y". The effective address is formed by adding the
	 * contents of Y to the address contained in the second and third bytes of the
	 * instruction. This mode allows the index register to contain the index or
	 * count value and instruction to contain the base address. This type of
	 * indexing allows any location referencing and thin index to modify fields,
	 * resulting in reduced coding and execution time.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> absoluteY() {
		return (cpu) -> {
			cpu.incPC();
			int lowBytes = cpu.read(cpu.getProgramCounter());
			int highBytes = cpu.read(cpu.getProgramCounter() + 1);
			cpu.handlingData = (highBytes << 8) | lowBytes;
			cpu.handlingData += cpu.getY();
			cpu.incPC();

			cpu.handlingData &= 0xffff;

			if ((cpu.handlingData & 0xff00) != (highBytes << 8))
				return 1;

			return 0;
		};
	}

	// Indirect addressing uses an absolute address to look up another address.
	/**
	 * bThe secondyte of the instruction contains the low order eight bits of a
	 * memory location. The high order eight bits of that memory location are
	 * contained in the third byte of the instruction. The contents of the fully
	 * specified memory location are the low order byte of the effective address.
	 * The next memory location contains the high order byte of the effective
	 * address which is loaded into the sixteen bits of the program counter. (JMP
	 * (ABS) only.)
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> indirect() {
		return (cpu) -> {
			// Get the pointer.
			cpu.incPC();
			int lowBytes = cpu.read(cpu.getProgramCounter());
			int highBytes = cpu.read(cpu.getProgramCounter() + 1);
			int pointer = (highBytes << 8) | lowBytes;
			cpu.incPC();

			// Simualte hardware bug.
			if (lowBytes == 0xff) {
				lowBytes = cpu.read(pointer);
				highBytes = cpu.read(pointer & 0xff00);
				cpu.handlingData = (highBytes << 8) | lowBytes;
			} else {
				lowBytes = cpu.read(pointer);
				highBytes = cpu.read(pointer + 1);
				cpu.handlingData = (highBytes << 8) | lowBytes;
			}

			return 0;
		};
	}

	/**
	 * In indexed indirect addressing (referred to as (Indirect, X)), the second
	 * byte of the instruction is added to the contents of the X register,
	 * discarding the carry. The result of this addition points to a memory location
	 * on page zero whose contents are the low order eight bits of the effective
	 * address. the next memory location in page zero contains the high order eight
	 * bits of the effective address. Both memory locations specifying the high and
	 * low order bytes of the effective address must be in page zero.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> indirectX() {
		return (cpu) -> {
			cpu.incPC();
			int temp = cpu.read(cpu.getProgramCounter());

			int lowBytes = cpu.read((temp + cpu.getX()) % 256);
			int highBytes = cpu.read((temp + cpu.getX() + 1) % 256);
			cpu.handlingData = (highBytes << 8) | lowBytes;

			return 0;
		};
	}

	/**
	 * In indirect indexed addressing (r eferred to as (Indirect), Y), the second
	 * byte of the instruction points to a memory location in page zero. The
	 * contents of this memory location are added to the contents of the Y index
	 * register, the result being the low order eight bits of the effective address.
	 * The carry from this addition is added to the contents of the next page zero
	 * memory location, the result being the high order eight bits of the effective
	 * address.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> indirectY() {
		return (cpu) -> {
			cpu.incPC();
			int pointer = cpu.read(cpu.getProgramCounter());

			int lowBytes = cpu.read(pointer & 0x00ff);
			int highBytes = cpu.read((pointer + 1) & 0x00ff);

			cpu.handlingData = (highBytes << 8) | lowBytes;
			cpu.handlingData += cpu.getY();

			cpu.handlingData &= 0xffff;
			if ((cpu.handlingData & 0xff00) != (highBytes << 8))
				return 1;

			return 0;
		};
	}
	// ===================================
	// HELPERS
	// ===================================

	// Get the value to perform operation on. Mainly used on arithmetic
	private static int fetchData(CPU6502 cpu) {
		int t;
		if (cpu.getCurrentInstruction().modeName == "implied")
			t = cpu.handlingData;
		else
			t = cpu.read(cpu.handlingData);
		return t;
	}

	/**
	 * Called when the opcode does not match any instruction.
	 * 
	 * @return - An <Executable> used to execute the operation and mode of every
	 *         instruction.
	 */
	public static Executable<CPU6502> empty() {
		return (cpu) -> {

			return 0;
		};
	}

	public static void main(String[] arg) {
		// Instruction ADC = new Instruction();
		// ADC.instruction = Instruction.ADC();

	}
}