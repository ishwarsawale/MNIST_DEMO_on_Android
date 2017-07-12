package mariannelinhares.mnistandroid.models

interface Classifier {
    fun name(): String

    fun recognize(pixels: FloatArray): Classification
}
