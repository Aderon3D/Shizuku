package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

public interface IPackageManager extends IInterface {
    @RequiresApi(37)
    PackageInfoList getInstalledPackages(long flags, int userId)
            throws RemoteException;

    abstract class Stub extends Binder implements IPackageManager {
        public static IPackageManager asInterface(IBinder ignoredObj) {
            throw new RuntimeException();
        }
    }
}
