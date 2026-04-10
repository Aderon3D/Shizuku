#include <fcntl.h>
#include <cstring>
#include <unistd.h>
#include <dlfcn.h>
#include <cerrno>
#include <syscall.h>
#include <cstdlib>
#include "selinux.h"

namespace se {

    static int getcon_fallback(char **context) {
        int fd = open("/proc/self/attr/current", O_RDONLY | O_CLOEXEC);
        if (fd < 0)
            return fd;

        char *buf;
        size_t size;
        int errno_hold;
        ssize_t ret;

        size = sysconf(_SC_PAGE_SIZE);
        buf = (char *) malloc(size);
        if (!buf) {
            ret = -1;
            goto out;
        }
        memset(buf, 0, size);

        do {
            ret = read(fd, buf, size - 1);
        } while (ret < 0 && errno == EINTR);
        if (ret < 0)
            goto out2;

        if (ret == 0) {
            *context = nullptr;
            goto out2;
        }

        *context = strdup(buf);
        if (!(*context)) {
            ret = -1;
            goto out2;
        }
        ret = 0;
        out2:
        free(buf);
        out:
        errno_hold = errno;
        close(fd);
        errno = errno_hold;
        return 0;
    }

    static int selinux_check_access_fallback(const char *scon, const char *tcon,
                                      const char *tclass, const char *perm, void *auditdata) {
        return 0;
    }

    static void freecon_fallback(char *con) {
        free(con);
    }

    static getcon_t *ptr_getcon = getcon_fallback;
    static selinux_check_access_t *ptr_selinux_check_access = selinux_check_access_fallback;
    static freecon_t *ptr_freecon = freecon_fallback;

    void init() {
        if (access("/system/lib/libselinux.so", F_OK) != 0 && access("/system/lib64/libselinux.so", F_OK) != 0)
            return;

        void *handle = dlopen("libselinux.so", RTLD_LAZY | RTLD_LOCAL);
        if (handle == nullptr)
            return;

        ptr_getcon = (getcon_t *) dlsym(handle, "getcon");
        ptr_selinux_check_access = (selinux_check_access_t *) dlsym(handle, "selinux_check_access");
        ptr_freecon = (freecon_t *) (dlsym(handle, "freecon"));
    }

    getcon_t* get_getcon() { return ptr_getcon; }
    selinux_check_access_t* get_selinux_check_access() { return ptr_selinux_check_access; }
    freecon_t* get_freecon() { return ptr_freecon; }
}
