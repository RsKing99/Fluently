package dev.karmakrafts.fluently.frontend

import org.intellij.lang.annotations.Language

@Language("fluent")
internal const val FLUENT_EXAMPLE: String = $$"""
## Closing tabs

tabs-close-button = Close
tabs-close-tooltip = {$tabCount ->
    [one] Close {$tabCount} tab
   *[other] Close {$tabCount} tabs
}
tabs-close-warning =  {$tabCount ->
    [one] You are about to close {$tabCount} tab.
          Are you sure you want to continue?
   *[other] You are about to close {$tabCount} tabs.
            Are you sure you want to continue?
}
    .some-attribute = Hello World!

## Syncing

-sync-brand-name = Firefox Account
    .some-attribute = Hello World!

sync-dialog-title = {-sync-brand-name}
sync-headline-title =
    {-sync-brand-name}: The best way to bring
    your data always with you
sync-signedout-title =
    Connect with your {-sync-brand-name}
"""