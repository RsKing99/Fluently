package dev.karmakrafts.fluently.frontend

import org.intellij.lang.annotations.Language

@Language("fluent")
internal const val FLUENT_EXAMPLE: String = $$"""
# Try editing the translations below.
# Set $variables' values in the Config tab.

-some-term = HELLO
 
shared-photos =
    {-some-term}, WORLD!
    {$userName} {$photoCount ->
        [one] added a new photo
        testing
       *[other] added {$photoCount} new photos
    } to {$userGender ->
        [male] {"*[male]\u0010\n\t"} his stream
        [female] her stream
       *[other] their stream
    }.
    .attrib = HELLOU
"""