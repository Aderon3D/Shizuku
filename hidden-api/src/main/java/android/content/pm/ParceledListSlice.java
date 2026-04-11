package android.content.pm;

import java.util.List;

public class ParceledListSlice<T> extends BaseParceledListSlice<T> {

    public ParceledListSlice(List<T> ignoredList) {
        throw new RuntimeException();
    }
}
