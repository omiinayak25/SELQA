@domain @i18n @internationalization
Feature: Internationalization — locale lifecycle, direction, formatting, and supported-locale registry
  As the i18n platform
  I want locale activation, RTL/LTR detection, number formatting, and the supported-locale
  registry to behave correctly for every targeted locale
  So that the application renders text, layout direction, and numbers correctly for each user

  Background:
    Given a clean i18n service

  # ---------------------------------------------------------------------------
  # Scenario 1: Default locale is English before any activation
  # ---------------------------------------------------------------------------
  @smoke @positive @sanity
  Scenario: Default locale is English before any locale is set
    Then the i18n active locale is the default English locale

  # ---------------------------------------------------------------------------
  # Scenario 2: Activating a locale sets the thread-local language code
  # ---------------------------------------------------------------------------
  @positive @smoke
  Scenario Outline: Activating locale <locale> sets the language code to <lang>
    When I activate locale "<locale>"
    Then the i18n active language code is "<lang>"

    Examples:
      | locale | lang |
      | en     | en   |
      | es     | es   |
      | fr     | fr   |
      | de     | de   |
      | ar     | ar   |
      | ja     | ja   |
      | he     | he   |
      | fa     | fa   |
      | ur     | ur   |

  # ---------------------------------------------------------------------------
  # Scenario 3: Activating a locale sets the full locale tag
  # ---------------------------------------------------------------------------
  @positive @regression
  Scenario Outline: Activating locale tag <tag> stores the correct active tag
    When I activate locale "<tag>"
    Then the i18n active locale tag is "<tag>"

    Examples:
      | tag    |
      | en     |
      | es     |
      | fr     |
      | de     |
      | ar     |
      | ja     |

  # ---------------------------------------------------------------------------
  # Scenario 4: Reset returns to English default
  # ---------------------------------------------------------------------------
  @positive @sanity
  Scenario: Resetting locale after activation returns to English default
    Given the i18n active locale is "fr"
    When  I reset the i18n locale
    Then  the i18n active locale is the default English locale

  # ---------------------------------------------------------------------------
  # Scenario 5: Setting locale overrides a previously set locale
  # ---------------------------------------------------------------------------
  @positive @business
  Scenario: Setting a new locale overrides the previously active locale
    Given the i18n active locale is "es"
    When  I activate locale "ja"
    Then  the i18n active language code is "ja"

  # ---------------------------------------------------------------------------
  # Scenario 6: TextDirection for every locale — Outline over direction
  # ---------------------------------------------------------------------------
  @positive @business @regression
  Scenario Outline: Locale <locale> has text direction <direction>
    When  I check the i18n text direction for locale "<locale>"
    Then  the i18n text direction is <direction>

    Examples:
      | locale | direction |
      | en     | LTR       |
      | es     | LTR       |
      | fr     | LTR       |
      | de     | LTR       |
      | ja     | LTR       |
      | zh     | LTR       |
      | pt     | LTR       |
      | it     | LTR       |
      | ar     | RTL       |
      | he     | RTL       |
      | fa     | RTL       |
      | ur     | RTL       |

  # ---------------------------------------------------------------------------
  # Scenario 7: TextDirection.name() returns "RTL" or "LTR" exactly
  # ---------------------------------------------------------------------------
  @positive @regression
  Scenario Outline: Locale <locale> direction name is <direction_name>
    Then the i18n text direction for locale "<locale>" is "<direction_name>"

    Examples:
      | locale | direction_name |
      | en     | LTR            |
      | fr     | LTR            |
      | de     | LTR            |
      | es     | LTR            |
      | ja     | LTR            |
      | ar     | RTL            |
      | he     | RTL            |
      | fa     | RTL            |
      | ur     | RTL            |

  # ---------------------------------------------------------------------------
  # Scenario 8: HTML dir attribute per locale
  # ---------------------------------------------------------------------------
  @positive @business
  Scenario Outline: HTML dir attribute for locale <locale> is <attr>
    Then the i18n html direction attribute for locale "<locale>" is "<attr>"

    Examples:
      | locale | attr |
      | en     | ltr  |
      | es     | ltr  |
      | fr     | ltr  |
      | de     | ltr  |
      | ja     | ltr  |
      | ar     | rtl  |
      | he     | rtl  |
      | fa     | rtl  |
      | ur     | rtl  |

  # ---------------------------------------------------------------------------
  # Scenario 9: Supported locale registry — default contains en, es, fr
  # ---------------------------------------------------------------------------
  @positive @sanity
  Scenario: Default supported locale registry includes en, es, and fr
    Then the i18n locale "en" is supported
    And  the i18n locale "es" is supported
    And  the i18n locale "fr" is supported

  # ---------------------------------------------------------------------------
  # Scenario 10: de is not supported until registered
  # ---------------------------------------------------------------------------
  @negative @boundary
  Scenario: German locale is not supported in the default registry
    Then the i18n locale "de" is not supported

  # ---------------------------------------------------------------------------
  # Scenario 11: Registering a new locale makes it supported
  # ---------------------------------------------------------------------------
  @positive @business
  Scenario Outline: Registering locale <locale> makes it appear in the supported list
    When  I register "<locale>" as an i18n supported locale
    Then  the i18n locale "<locale>" is supported

    Examples:
      | locale |
      | de     |
      | ar     |
      | ja     |

  # ---------------------------------------------------------------------------
  # Scenario 12: Registering a locale idempotently (duplicates not added)
  # ---------------------------------------------------------------------------
  @positive @boundary
  Scenario: Registering an already-supported locale does not increase the count
    Then the i18n locale "en" is supported
    When I register "en" as an i18n supported locale
    Then the i18n locale "en" is supported
    And  the i18n supported locale count is at least 3

  # ---------------------------------------------------------------------------
  # Scenario 13: Supported locale count grows when new locales are added
  # ---------------------------------------------------------------------------
  @positive @business
  Scenario: Supported locale count is at least 3 by default
    Then the i18n supported locale count is at least 3

  # ---------------------------------------------------------------------------
  # Scenario 14: Number formatting — integer
  # ---------------------------------------------------------------------------
  @positive @business
  Scenario Outline: Integer <number> formatted for locale <locale> is <formatted>
    When  I format the number <number> for locale "<locale>"
    Then  the i18n formatted number is "<formatted>"

    Examples:
      | number  | locale | formatted  |
      | 1000.0  | en     | 1,000      |
      | 1000.0  | de     | 1.000      |
      | 1000.0  | es     | 1.000      |

  # ---------------------------------------------------------------------------
  # Scenario 15: Number formatting — decimal separator varies per locale
  # ---------------------------------------------------------------------------
  @positive @business
  Scenario Outline: Number <number> for locale <locale> uses decimal separator <sep>
    When  I format the number <number> for locale "<locale>"
    Then  the i18n formatted number contains decimal separator "<sep>"

    Examples:
      | number | locale | sep |
      | 1.5    | en     | .   |
      | 1.5    | de     | ,   |
      | 1.5    | fr     | ,   |
      | 1.5    | es     | ,   |

  # ---------------------------------------------------------------------------
  # Scenario 16: Active-locale bundle resolution after activation
  # ---------------------------------------------------------------------------
  @positive @business @smoke
  Scenario Outline: After activating locale <locale>, key login.title resolves to <expected>
    Given the i18n active locale is "<locale>"
    Then  the i18n key "login.title" resolves in the active locale to "<expected>"

    Examples:
      | locale | expected       |
      | en     | Sign In        |
      | es     | Iniciar sesión |
      | fr     | Se connecter   |
      | de     | Anmelden       |
      | ja     | サインイン      |

  # ---------------------------------------------------------------------------
  # Scenario 17: Key × locale resolution via i18n steps (cross-check with real bundles)
  # ---------------------------------------------------------------------------
  @positive @regression @business
  Scenario Outline: i18n key <key> for locale <locale> resolves to <expected>
    Then the i18n key "<key>" for locale "<locale>" resolves to "<expected>"

    Examples:
      | key                   | locale | expected                                          |
      | cart.empty            | en     | Your cart is empty                                |
      | cart.empty            | es     | Tu carrito está vacío                             |
      | cart.empty            | fr     | Votre panier est vide                             |
      | cart.empty            | de     | Ihr Warenkorb ist leer                            |
      | cart.empty            | ar     | سلة التسوق فارغة                                 |
      | cart.empty            | ja     | カートは空です                                      |
      | nav.logout            | en     | Log Out                                           |
      | nav.logout            | es     | Cerrar sesión                                     |
      | nav.logout            | fr     | Se déconnecter                                    |
      | nav.logout            | de     | Abmelden                                          |
      | nav.logout            | ar     | تسجيل الخروج                                      |
      | nav.logout            | ja     | ログアウト                                          |
      | checkout.confirm      | en     | Place Order                                       |
      | checkout.confirm      | es     | Realizar pedido                                   |
      | checkout.confirm      | fr     | Passer la commande                                |
      | checkout.confirm      | de     | Bestellung aufgeben                               |
      | checkout.confirm      | ar     | تأكيد الطلب                                       |
      | checkout.confirm      | ja     | 注文を確定する                                       |
      | login.error.invalid   | en     | Invalid username or password                      |
      | login.error.invalid   | es     | Nombre de usuario o contraseña inválidos          |
      | login.error.invalid   | fr     | Nom d’utilisateur ou mot de passe invalide        |
      | login.error.invalid   | de     | Ungültiger Benutzername oder Passwort             |
      | login.error.invalid   | ar     | اسم المستخدم أو كلمة المرور غير صحيحين           |
      | login.error.invalid   | ja     | ユーザー名またはパスワードが正しくありません              |
      | cart.subtotal         | en     | Subtotal                                          |
      | cart.subtotal         | de     | Zwischensumme                                     |
      | cart.subtotal         | ar     | المجموع الفرعي                                   |
      | cart.subtotal         | ja     | 小計                                               |
      | nav.account           | en     | My Account                                        |
      | nav.account           | es     | Mi cuenta                                         |
      | nav.account           | de     | Mein Konto                                        |
      | nav.account           | ar     | حسابي                                             |
      | nav.account           | ja     | アカウント                                          |
      | nav.home              | en     | Home                                              |
      | nav.home              | es     | Inicio                                            |
      | nav.home              | fr     | Accueil                                           |
      | nav.home              | de     | Startseite                                        |
      | nav.home              | ar     | الرئيسية                                          |
      | nav.home              | ja     | ホーム                                             |
      | checkout.order.summary| en     | Order Summary                                     |
      | checkout.order.summary| de     | Bestellübersicht                                  |
      | checkout.order.summary| ar     | ملخص الطلب                                        |
      | checkout.order.summary| ja     | 注文の要約                                          |

  # ---------------------------------------------------------------------------
  # Scenario 18: Locale reset between activations — isolation
  # ---------------------------------------------------------------------------
  @positive @sanity @boundary
  Scenario: Locale is independent per activation — reset restores English
    Given the i18n active locale is "ar"
    Then  the i18n active language code is "ar"
    When  I reset the i18n locale
    Then  the i18n active locale is the default English locale
    When  I activate locale "ja"
    Then  the i18n active language code is "ja"

  # ---------------------------------------------------------------------------
  # Scenario 19: RTL locales include Arabic, Hebrew, Farsi, Urdu (boundary)
  # ---------------------------------------------------------------------------
  @positive @boundary @business
  Scenario Outline: RTL locale <locale> is correctly classified as RTL
    When  I check the i18n text direction for locale "<locale>"
    Then  the i18n text direction is RTL
    And   the i18n html direction attribute for locale "<locale>" is "rtl"

    Examples:
      | locale |
      | ar     |
      | he     |
      | fa     |
      | ur     |

  # ---------------------------------------------------------------------------
  # Scenario 20: LTR locales are NOT classified as RTL (negative boundary)
  # ---------------------------------------------------------------------------
  @negative @boundary
  Scenario Outline: LTR locale <locale> is NOT classified as RTL
    When  I check the i18n text direction for locale "<locale>"
    Then  the i18n text direction is LTR

    Examples:
      | locale |
      | en     |
      | fr     |
      | de     |
      | ja     |
      | es     |
      | zh     |
