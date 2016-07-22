package net.onrc.openvirtex.services;

/**
 * Created by Administrator on 2016-07-04.
 */
public class MplsLabel {

    private final int mplsLabel;

    // An MPLS Label maximum 20 bits.
    public static final int MAX_MPLS = 0xFFFFF;
    public static int mpls_index = 0;

    protected MplsLabel(int value) {
        this.mplsLabel = value;
    }

    public static MplsLabel mplsLabel(int value) {

        if (value < 0 || value > MAX_MPLS) {
            String errorMsg = "MPLS label value " + value +
                    " is not in the interval [0, 0xFFFFF]";
            throw new IllegalArgumentException(errorMsg);
        }
        return new MplsLabel(value);
    }

    public int toInt() {
        return this.mplsLabel;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof MplsLabel) {

            MplsLabel other = (MplsLabel) obj;

            if (this.mplsLabel == other.mplsLabel) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.mplsLabel;
    }

    @Override
    public String toString() {
        return String.valueOf(this.mplsLabel);
    }
}