package cpu;

// 6502 flags. Processor Status
//	NV-BDIZC
//	00110011

public enum Flag {
	CARRY(1), ZERO(1 << 1), INTERRUPT_DISABLE(1 << 2), DECIMAL_MODE(1 << 3), BREAK(1 << 4), UNUSED(1 << 5), OVERFLOW(1 << 6), NEGATIVE(1 << 7);

	private final int flagBit;

	Flag(int bit) {
		this.flagBit = bit;
	}

	public int getFlag() {
		return flagBit;
	}
}