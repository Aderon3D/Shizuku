#ifndef SELINUX_H
#define SELINUX_H

namespace se {
    void init();

    using getcon_t = int(char **);
    using selinux_check_access_t = int(const char *, const char *, const char *, const char *,
                                       void *);
    using freecon_t = void(char *);

    getcon_t* get_getcon();
    selinux_check_access_t* get_selinux_check_access();
    freecon_t* get_freecon();
}

#endif // SELINUX_H
