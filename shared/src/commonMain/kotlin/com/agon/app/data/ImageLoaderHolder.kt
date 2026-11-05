package com.agon.app.data

import coil3.ImageLoader

/** Global Coil ImageLoader configured per-platform at startup. */
object ImageLoaderHolder {
    var loader: ImageLoader? = null
}
