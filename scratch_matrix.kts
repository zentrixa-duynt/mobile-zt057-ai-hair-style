import android.graphics.Matrix

val m = Matrix()
m.setTranslate(10f, 20f)
val t = Matrix()
t.setScale(2f, 2f)

m.preConcat(t)
val vals = FloatArray(9)
m.getValues(vals)
println(vals.joinToString())
