DESCRIPTION = "libGLES for the A10/A13 Allwinner processor with Mali 400 (X11)"

LICENSE = "proprietary-binary"
LIC_FILES_CHKSUM = "file://README;md5=1b81a178e80ee888ee4571772699ab2c"

COMPATIBLE_MACHINE = "(mele|meleg|cubieboard|cubieboard2|cubietruck|olinuxino-a10|olinuxino-a13|olinuxino-a20)"

DEPENDS = "virtual/libx11 libxau libxdmcp libdrm dri2proto libdri2 libump"

# These libraries shouldn't get installed in world builds unless something
# explicitly depends upon them.
EXCLUDE_FROM_WORLD = "1"
PROVIDES = "virtual/libgles1 virtual/libgles2 virtual/egl"

inherit distro_features_check
REQUIRED_DISTRO_FEATURES = "opengl"

SRCREV_pn-${PN} = "95bbd40135f96b473d4c713317e485d0049580cd"
SRC_URI = "gitsm://github.com/raoulh/sunxi-mali.git"

S = "${WORKDIR}/git"

DEPENDS = "libdrm dri2proto libump"

PACKAGECONFIG ??= "${@base_contains('DISTRO_FEATURES', 'x11', 'x11', '', d)} ${@base_contains('DISTRO_FEATURES', 'wayland', 'wayland', '', d)}"
PACKAGECONFIG[wayland] = "EGL_TYPE=framebuffer,,,"
PACKAGECONFIG[x11] = "EGL_TYPE=x11,,virtual/libx11 libxau libxdmcp libdri2,"

do_configure() {
         DESTDIR=${D}/ VERSION=r3p0 ABI=armhf EGL_TYPE=x11 make config
}

do_install() {

    make -f Makefile.pc

    # install headers
    install -d -m 0755 ${D}${includedir}/EGL
    install -m 0755 ${S}/include/EGL/*.h ${D}${includedir}/EGL/
    install -d -m 0755 ${D}${includedir}/GLES
    install -m 0755 ${S}/include/GLES/*.h ${D}${includedir}/GLES/
    install -d -m 0755 ${D}${includedir}/GLES2
    install -m 0755 ${S}/include/GLES2/*.h ${D}${includedir}/GLES2/
    install -d -m 0755 ${D}${includedir}/KHR
    install -m 0755 ${S}/include/KHR/*.h ${D}${includedir}/KHR/

    # Copy the .pc files
    install -d -m 0755 ${D}${libdir}/pkgconfig
    install -m 0644 ${S}/egl.pc ${D}${libdir}/pkgconfig/
    install -m 0644 ${S}/gles_cm.pc ${D}${libdir}/pkgconfig/
    install -m 0644 ${S}/glesv2.pc ${D}${libdir}/pkgconfig/
	     
    install -d ${D}${libdir}
    install -d ${D}${includedir}
    
    make libdir=${D}${libdir}/ includedir=${D}${includedir}/ install
    make libdir=${D}${libdir}/ includedir=${D}${includedir}/ install -C include

    # Fix .so name and create symlinks, binary package provides .so wich can't be included directly in package without triggering the 'dev-so' QA check
    # Packages like xf86-video-fbturbo dlopen() libUMP.so, so we do need to ship the .so files in ${PN}

    mv ${D}${libdir}/libMali.so ${D}${libdir}/libMali.so.3
    ln -sf libMali.so.3 ${D}${libdir}/libMali.so

    for flib in libEGL.so.1.4 libGLESv1_CM.so.1.1 libGLESv2.so.2.0 ; do
        rm ${D}${libdir}/$flib
        ln -sf libMali.so.3 ${D}${libdir}/$flib
    done
}

# Packages like xf86-video-fbturbo dlopen() libUMP.so, so we do need to ship the .so files in ${PN}
FILES_${PN} += "${libdir}/lib*.so"
FILES_${PN}-dev = "${includedir}"
# These are closed binaries generated elsewhere so don't check ldflags & text relocations
INSANE_SKIP_${PN} = "dev-so ldflags textrel"

# Inhibit warnings about files being stripped, we can't do anything about it.
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"