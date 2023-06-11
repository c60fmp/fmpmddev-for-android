package jp.fmp.c60.fmpmddev;

public class MutableInt {
    int value;

    public void setValue(int newval)
    {
        this.value = newval;
    }

    public int getValue()
    {
        return this.value;
    }
}
