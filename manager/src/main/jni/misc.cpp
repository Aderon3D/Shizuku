#include <sys/types.h>
#include <sys/sendfile.h>
#include <sys/stat.h>
#include <zconf.h>
#include <dirent.h>
#include <fcntl.h>
#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <sched.h>
#include <cctype>
#include <cerrno>
#include "misc.h"

ssize_t fdgets(char *buf, const size_t size, int fd) {
    buf[0] = '\0';
    ssize_t ret;
    do {
        ret = read(fd, buf, size - 1);
    } while (ret < 0 && errno == EINTR);
    if (ret < 0)
        return -1;
    buf[ret] = '\0';
    return ret;
}

int get_proc_name(int pid, char *name, size_t size) {
    int fd;
    char buf[PATH_MAX];
    snprintf(buf, sizeof(buf), "/proc/%d/cmdline", pid);
    if ((fd = open(buf, O_RDONLY)) == -1)
        return 1;
    fdgets(name, size, fd);
    close(fd);
    return 0;
}

int switch_mnt_ns(int pid) {
    char mnt[32];
    snprintf(mnt, sizeof(mnt), "/proc/%d/ns/mnt", pid);
    if (access(mnt, R_OK) == -1) return -1;

    int fd = open(mnt, O_RDONLY);
    if (fd < 0) return -1;

    int res = setns(fd, 0);
    close(fd);
    return res;
}

void foreach_proc(foreach_proc_function *func) {
    DIR *dir;
    struct dirent *entry;

    if (!(dir = opendir("/proc")))
        return;

    while ((entry = readdir(dir))) {
        if (entry->d_type != DT_DIR) continue;
        auto pid = static_cast<pid_t>(strtol(entry->d_name, nullptr, 10));
        func(pid);
    }

    closedir(dir);
}
