# kotlinx.serialization: keep generated serializers for our models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class ru.finny.pet.** {
    *** Companion;
}
-keepclasseswithmembers class ru.finny.pet.** {
    kotlinx.serialization.KSerializer serializer(...);
}
