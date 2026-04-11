package android.content.pm;

import java.util.List;

public class PackageInfoList extends ParceledListSlice<PackageInfo> {
    public PackageInfoList(List<PackageInfo> list) {
        super(list);
    }
}
