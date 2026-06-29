@domain @i18n @localization
Feature: Localization — message bundle resolution and completeness
  As the i18n platform
  I want every message key to resolve to its correct translated value per locale
  So that users always see text in their own language without missing or fallback placeholders

  Background:
    Given a clean localization bundle

  # ---------------------------------------------------------------------------
  # Scenario 1: Base bundle (English) exists on the classpath
  # ---------------------------------------------------------------------------
  @smoke @positive @sanity
  Scenario: English base bundle is present on the classpath
    Then the localization bundle exists for locale "en"

  # ---------------------------------------------------------------------------
  # Scenario 2-6: Bundles exist for every supported locale
  # ---------------------------------------------------------------------------
  @positive @regression
  Scenario Outline: Bundle exists for each supported locale
    Then the localization bundle exists for locale "<locale>"

    Examples:
      | locale |
      | en     |
      | es     |
      | fr     |
      | de     |
      | ar     |
      | ja     |

  # ---------------------------------------------------------------------------
  # Scenario 7: Bundle is absent for an unsupported locale
  # ---------------------------------------------------------------------------
  @negative @boundary
  Scenario: Bundle is absent for an unsupported locale
    Then the localization bundle is absent for locale "zz"

  # ---------------------------------------------------------------------------
  # Scenario 8: Key count per locale matches the base (16 keys)
  # ---------------------------------------------------------------------------
  @positive @regression
  Scenario Outline: Each locale bundle contains all 16 defined keys
    Then the localization locale "<locale>" has <count> keys

    Examples:
      | locale | count |
      | en     | 16    |
      | es     | 16    |
      | fr     | 16    |
      | de     | 16    |
      | ar     | 16    |
      | ja     | 16    |

  # ---------------------------------------------------------------------------
  # Scenario 9: Fully-translated assertion per locale
  # ---------------------------------------------------------------------------
  @positive @regression
  Scenario Outline: Each locale is fully translated against English
    Then the localization locale "<locale>" is fully translated

    Examples:
      | locale |
      | es     |
      | fr     |
      | de     |
      | ar     |
      | ja     |

  # ---------------------------------------------------------------------------
  # Scenario 10: No missing keys per locale
  # ---------------------------------------------------------------------------
  @positive @regression
  Scenario Outline: No keys are missing relative to English for each locale
    Then the localization locale "<locale>" has no missing keys compared to English

    Examples:
      | locale |
      | es     |
      | fr     |
      | de     |
      | ar     |
      | ja     |

  # ---------------------------------------------------------------------------
  # Scenario 11: Key presence for every key × locale combination
  # ---------------------------------------------------------------------------
  @positive @business
  Scenario Outline: Key <key> is present for locale <locale>
    Then the localization key "<key>" is present for locale "<locale>"

    Examples:
      | key                   | locale |
      | login.title           | en     |
      | login.title           | es     |
      | login.title           | fr     |
      | login.title           | de     |
      | login.title           | ar     |
      | login.title           | ja     |
      | login.username.label  | en     |
      | login.username.label  | es     |
      | login.username.label  | fr     |
      | login.username.label  | de     |
      | login.username.label  | ar     |
      | login.username.label  | ja     |
      | login.password.label  | en     |
      | login.password.label  | es     |
      | login.password.label  | fr     |
      | login.password.label  | de     |
      | login.password.label  | ar     |
      | login.password.label  | ja     |
      | login.button          | en     |
      | login.button          | es     |
      | login.button          | fr     |
      | login.button          | de     |
      | login.button          | ar     |
      | login.button          | ja     |
      | login.error.invalid   | en     |
      | login.error.invalid   | es     |
      | login.error.invalid   | fr     |
      | login.error.invalid   | de     |
      | login.error.invalid   | ar     |
      | login.error.invalid   | ja     |
      | cart.empty            | en     |
      | cart.empty            | es     |
      | cart.empty            | fr     |
      | cart.empty            | de     |
      | cart.empty            | ar     |
      | cart.empty            | ja     |
      | cart.item.count       | en     |
      | cart.item.count       | es     |
      | cart.item.count       | fr     |
      | cart.item.count       | de     |
      | cart.item.count       | ar     |
      | cart.item.count       | ja     |
      | cart.subtotal         | en     |
      | cart.subtotal         | es     |
      | cart.subtotal         | fr     |
      | cart.subtotal         | de     |
      | cart.subtotal         | ar     |
      | cart.subtotal         | ja     |
      | checkout.button       | en     |
      | checkout.button       | es     |
      | checkout.button       | fr     |
      | checkout.button       | de     |
      | checkout.button       | ar     |
      | checkout.button       | ja     |
      | checkout.title        | en     |
      | checkout.title        | es     |
      | checkout.title        | fr     |
      | checkout.title        | de     |
      | checkout.title        | ar     |
      | checkout.title        | ja     |
      | checkout.order.summary| en     |
      | checkout.order.summary| es     |
      | checkout.order.summary| fr     |
      | checkout.order.summary| de     |
      | checkout.order.summary| ar     |
      | checkout.order.summary| ja     |
      | checkout.confirm      | en     |
      | checkout.confirm      | es     |
      | checkout.confirm      | fr     |
      | checkout.confirm      | de     |
      | checkout.confirm      | ar     |
      | checkout.confirm      | ja     |
      | nav.home              | en     |
      | nav.home              | es     |
      | nav.home              | fr     |
      | nav.home              | de     |
      | nav.home              | ar     |
      | nav.home              | ja     |
      | nav.shop              | en     |
      | nav.shop              | es     |
      | nav.shop              | fr     |
      | nav.shop              | de     |
      | nav.shop              | ar     |
      | nav.shop              | ja     |
      | nav.account           | en     |
      | nav.account           | es     |
      | nav.account           | fr     |
      | nav.account           | de     |
      | nav.account           | ar     |
      | nav.account           | ja     |
      | nav.logout            | en     |
      | nav.logout            | es     |
      | nav.logout            | fr     |
      | nav.logout            | de     |
      | nav.logout            | ar     |
      | nav.logout            | ja     |

  # ---------------------------------------------------------------------------
  # Scenario 12: Exact translation values — key × locale → expected text
  # ---------------------------------------------------------------------------
  @positive @business @regression
  Scenario Outline: Key <key> resolves to the correct translation for locale <locale>
    Then the localization key "<key>" for locale "<locale>" is "<expected>"

    Examples:
      | key                  | locale | expected                                              |
      | login.title          | en     | Sign In                                               |
      | login.title          | es     | Iniciar sesión                                        |
      | login.title          | fr     | Se connecter                                          |
      | login.title          | de     | Anmelden                                              |
      | login.title          | ar     | تسجيل الدخول                                         |
      | login.title          | ja     | サインイン                                             |
      | login.button         | en     | Log In                                                |
      | login.button         | es     | Entrar                                                |
      | login.button         | fr     | Connexion                                             |
      | login.button         | de     | Einloggen                                             |
      | login.button         | ar     | دخول                                                  |
      | login.button         | ja     | ログイン                                               |
      | cart.empty           | en     | Your cart is empty                                    |
      | cart.empty           | es     | Tu carrito está vacío                                 |
      | cart.empty           | fr     | Votre panier est vide                                 |
      | cart.empty           | de     | Ihr Warenkorb ist leer                                |
      | cart.empty           | ar     | سلة التسوق فارغة                                     |
      | cart.empty           | ja     | カートは空です                                          |
      | cart.subtotal        | en     | Subtotal                                              |
      | cart.subtotal        | es     | Subtotal                                              |
      | cart.subtotal        | fr     | Sous-total                                            |
      | cart.subtotal        | de     | Zwischensumme                                         |
      | cart.subtotal        | ar     | المجموع الفرعي                                       |
      | cart.subtotal        | ja     | 小計                                                  |
      | checkout.button      | en     | Proceed to Checkout                                   |
      | checkout.button      | es     | Proceder al pago                                      |
      | checkout.button      | fr     | Passer à la caisse                                    |
      | checkout.button      | de     | Zur Kasse                                             |
      | checkout.button      | ar     | المتابعة إلى الدفع                                   |
      | checkout.button      | ja     | レジに進む                                             |
      | checkout.title       | en     | Checkout                                              |
      | checkout.title       | es     | Pago                                                  |
      | checkout.title       | fr     | Caisse                                                |
      | checkout.title       | de     | Kasse                                                 |
      | checkout.title       | ar     | الدفع                                                 |
      | checkout.title       | ja     | レジ                                                  |
      | checkout.confirm     | en     | Place Order                                           |
      | checkout.confirm     | es     | Realizar pedido                                       |
      | checkout.confirm     | fr     | Passer la commande                                    |
      | checkout.confirm     | de     | Bestellung aufgeben                                   |
      | checkout.confirm     | ar     | تأكيد الطلب                                           |
      | checkout.confirm     | ja     | 注文を確定する                                          |
      | nav.home             | en     | Home                                                  |
      | nav.home             | es     | Inicio                                                |
      | nav.home             | fr     | Accueil                                               |
      | nav.home             | de     | Startseite                                            |
      | nav.home             | ar     | الرئيسية                                              |
      | nav.home             | ja     | ホーム                                                 |
      | nav.shop             | en     | Shop                                                  |
      | nav.shop             | es     | Tienda                                                |
      | nav.shop             | fr     | Boutique                                              |
      | nav.shop             | de     | Shop                                                  |
      | nav.shop             | ar     | المتجر                                                |
      | nav.shop             | ja     | ショップ                                               |
      | nav.account          | en     | My Account                                            |
      | nav.account          | es     | Mi cuenta                                             |
      | nav.account          | fr     | Mon compte                                            |
      | nav.account          | de     | Mein Konto                                            |
      | nav.account          | ar     | حسابي                                                 |
      | nav.account          | ja     | アカウント                                              |
      | nav.logout           | en     | Log Out                                               |
      | nav.logout           | es     | Cerrar sesión                                         |
      | nav.logout           | fr     | Se déconnecter                                        |
      | nav.logout           | de     | Abmelden                                              |
      | nav.logout           | ar     | تسجيل الخروج                                          |
      | nav.logout           | ja     | ログアウト                                              |
      | login.username.label | en     | Username                                              |
      | login.username.label | es     | Nombre de usuario                                     |
      | login.username.label | fr     | Nom d’utilisateur                                     |
      | login.username.label | de     | Benutzername                                          |
      | login.username.label | ar     | اسم المستخدم                                          |
      | login.username.label | ja     | ユーザー名                                              |
      | login.password.label | en     | Password                                              |
      | login.password.label | es     | Contraseña                                            |
      | login.password.label | fr     | Mot de passe                                          |
      | login.password.label | de     | Passwort                                              |
      | login.password.label | ar     | كلمة المرور                                           |
      | login.password.label | ja     | パスワード                                              |
      | nav.logout           | de     | Abmelden                                              |
      | checkout.order.summary | en   | Order Summary                                         |
      | checkout.order.summary | es   | Resumen del pedido                                    |
      | checkout.order.summary | fr   | Récapitulatif de la commande                          |
      | checkout.order.summary | de   | Bestellübersicht                                      |
      | checkout.order.summary | ar   | ملخص الطلب                                            |
      | checkout.order.summary | ja   | 注文の要約                                              |

  # ---------------------------------------------------------------------------
  # Scenario 13: Fallback — a missing locale falls back to English
  # ---------------------------------------------------------------------------
  @positive @business @boundary
  Scenario Outline: Missing locale <locale> for key <key> falls back to the English value
    Then the localization key "<key>" for missing locale "<locale>" falls back to "<expected_en>"

    Examples:
      | key            | locale | expected_en        |
      | login.title    | zz     | Sign In            |
      | cart.empty     | zz     | Your cart is empty |
      | nav.home       | xx     | Home               |
      | checkout.title | yy     | Checkout           |

  # ---------------------------------------------------------------------------
  # Scenario 14: Truly unknown key returns !!key!! marker
  # ---------------------------------------------------------------------------
  @negative @boundary
  Scenario Outline: Completely unknown key returns the missing-key marker for locale <locale>
    Then the localization key "<key>" for locale "<locale>" returns the missing-key marker

    Examples:
      | key                        | locale |
      | does.not.exist             | en     |
      | does.not.exist             | es     |
      | totally.unknown.key        | fr     |
      | another.missing.key        | de     |
      | yet.another.missing.key    | ar     |

  # ---------------------------------------------------------------------------
  # Scenario 15: isPresent returns false for unknown key in real bundles
  # ---------------------------------------------------------------------------
  @negative @validation
  Scenario Outline: Key <key> is absent for locale <locale>
    Then the localization key "<key>" is absent for locale "<locale>"

    Examples:
      | key                   | locale |
      | no.such.key           | en     |
      | no.such.key           | es     |
      | absolutely.not.there  | fr     |
      | missing.translation   | de     |

  # ---------------------------------------------------------------------------
  # Scenario 16: Cache clear then reload — bundle still works
  # ---------------------------------------------------------------------------
  @positive @sanity
  Scenario: Clearing the bundle cache does not prevent subsequent loads
    When I clear the localization bundle cache
    Then the localization bundle for locale "en" still loads after cache clear
    And  the localization bundle for locale "de" still loads after cache clear
    And  the localization bundle for locale "ar" still loads after cache clear

  # ---------------------------------------------------------------------------
  # Scenario 17: getAllKeys returns non-empty set per locale
  # ---------------------------------------------------------------------------
  @positive @regression
  Scenario Outline: getAllKeys returns a non-empty set for locale <locale>
    Then the localization locale "<locale>" has all keys present against English

    Examples:
      | locale |
      | es     |
      | fr     |
      | de     |
      | ar     |
      | ja     |

  # ---------------------------------------------------------------------------
  # Scenario 18: Active locale via Given + key resolved
  # ---------------------------------------------------------------------------
  @positive @business
  Scenario Outline: Active locale <locale> resolves login.title to <expected>
    Given the localization locale is "<locale>"
    And   the localization key "login.title" is translated

    Examples:
      | locale | expected    |
      | en     | Sign In     |
      | es     | Iniciar sesión |
      | fr     | Se connecter   |
      | de     | Anmelden       |
      | ja     | サインイン      |

  # ---------------------------------------------------------------------------
  # Scenario 19: findMissingKeys returns empty for all fully-translated locales
  # ---------------------------------------------------------------------------
  @positive @regression
  Scenario Outline: findMissingKeys returns empty set for fully-translated locale <locale>
    Then the localization locale "<locale>" has no missing keys compared to English

    Examples:
      | locale |
      | es     |
      | fr     |
      | de     |
      | ar     |
      | ja     |

  # ---------------------------------------------------------------------------
  # Scenario 20: findMissingKeys returns non-empty for a fictitious partial locale
  # ---------------------------------------------------------------------------
  @negative @boundary
  Scenario: findMissingKeys detects missing keys for an unsupported locale
    Then the localization locale "zz" has missing keys compared to English

  # ---------------------------------------------------------------------------
  # Scenario 21: All keys present check on English itself (reflexive)
  # ---------------------------------------------------------------------------
  @positive @sanity
  Scenario: English locale has all keys present against itself
    Then the localization locale "en" has all keys present against English
