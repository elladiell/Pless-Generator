public class ShiftRegister {
    int size = 8;
    int register;

    int [] bitsForXor = new int[]{0, 4, 7, 5};

    public ShiftRegister(int register, int size) {
        this.register = register;
        this.size = size;
    }

    public ShiftRegister(int register, int size, int [] bitsForXor) {
        this.register = register;
        this.size = size;
        this.bitsForXor = bitsForXor;
    }

    boolean getLowBit(){
        return (register & 0x01) != 0;
    }

    void shift(){
//        boolean bit = getBit(bitsForXor[0]);
//        for (int i = 1; i < bitsForXor.length; i++) {
//            bit ^= getBit(bitsForXor[i]);
//        }

        //        boolean bit = getBit(0) ^ getBit(4) ^ getBit(7) ^ getBit(5) ;
//        boolean bit = getBit(0)  ^ getBit(2) ^ getBit(3) ^ getBit(4)^ getBit(5)^ getBit(6)^ getBit(7);
//        boolean bit = getBit(0)  ^ getBit(2) ^ getBit(3) ^ getBit(4) ^ getBit(6)^ getBit(7);
//        boolean bit = getBit(0)  ^ getBit(2) ^ getBit(3) ^ getBit(4) ^ getBit(6);
//        boolean bit = getBit(0)  ^ getBit(7) ^ getBit(4) ^ getBit(6);
//        boolean bit = getBit(0)  ^ getBit(7) ^ getBit(3);
        boolean bit = getBit(7) ^ getBit(6);  //  BEST 4 from 5 passes!!
//        boolean bit = getBit(7) ^ getBit(6) ^ getBit(0);
        register >>>= 1;
        setBit(7, bit);
    }

    private boolean getBit(int bitNo) {
        int mask = 0x01;
        for (int j = 0; j < bitNo; j++) {
            mask <<= 1;
        }
        return (register & mask) != 0;
    }
    private void setBit(int bitNo, boolean value) {
        int mask = 0x01;
        for (int j = 0; j < bitNo; j++) {
            mask <<= 1;
        }
        if(value) {
            register |= mask;
        }else{
            register &= ~mask;
        }
    }
}
