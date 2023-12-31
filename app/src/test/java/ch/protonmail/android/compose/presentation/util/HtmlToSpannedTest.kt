/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.compose.presentation.util

import androidx.core.text.HtmlCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test suite for [HtmlToSpanned]
 */
class HtmlToSpannedTest {

    private lateinit var htmlToSpannable: HtmlToSpanned

    @BeforeTest
    fun setup() {
        mockkStatic(HtmlCompat::class)
        every { HtmlCompat.fromHtml(any(), any()) } answers {
            mockk {
                every { this@mockk.toString() } returns firstArg()
            }
        }
        htmlToSpannable = HtmlToSpanned()
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(HtmlCompat::class)
    }

    @Test
    fun removesHeadFromHtml() {
        // given
        val input = HTML_WITH_CSS_IN_HEAD
        val expected = "Sent to myself"

        // when
        val result = htmlToSpannable(input).toString()

        // then
        assertEquals(expected, result)
    }
}

private val HTML_WITH_CSS_IN_HEAD = """
    |<head>
    | <style>/* for HTML 5 */
    |article,
    |aside,
    |datalist,
    |details,
    |dialog,
    |figure,
    |footer,
    |header,
    |main,
    |menu,
    |nav,
    |section {
    |  display: block;
    |}
    |
    |audio,
    |canvas,
    |progress,
    |video {
    |  display: inline-block;
    |}
    |
    |abbr,
    |mark,
    |meter,
    |time,
    |output {
    |  display: inline;
    |}
    |
    |html {
    |  box-sizing: border-box;
    |}
    |
    |*,
    |*::before,
    |*::after {
    |  box-sizing: inherit;
    |}
    |
    |html,
    |body,
    |blockquote,
    |ul,
    |ol,
    |form,
    |button,
    |figure {
    |  margin: 0;
    |  padding: 0;
    |}
    |
    |button,
    |progress {
    |  border: 0;
    |}
    |
    |p,
    |ul,
    |ol,
    |dl,
    |blockquote,
    |pre,
    |menu,
    |td,
    |th {
    |  font-size: 1em;
    |  line-height: 1.5;
    |  margin: 1.5em 0;
    |}
    |
    |input,
    |select,
    |textarea,
    |optgroup,
    |button {
    |  font: inherit;
    |}
    |
    |img,
    |iframe {
    |  vertical-align: middle;
    |}
    |
    |ul,
    |ol,
    |menu {
    |  padding-left: 2em;
    |}
    |
    |dd {
    |  margin-left: 2em;
    |}
    |
    |b,
    |strong {
    |  font-weight: bold;
    |}
    |
    |pre,
    |code,
    |kbd,
    |samp {
    |  font-family: SFMono-Regular, Consolas, "Liberation Mono", "Menlo", monospace,
    |    monospace;
    |  font-size: 1em;
    |}
    |
    |pre {
    |  white-space: pre-wrap;
    |  word-wrap: break-word;
    |}
    |
    |mark {
    |  background-color: var(--bgcolor-mark, #ff0);
    |  color: var(--color-mark, currentColor);
    |  font-weight: bold;
    |}
    |
    |small {
    |  font-size: 80%;
    |}
    |
    |a:link img,
    |a:visited img,
    |img {
    |  border-style: none;
    |}
    |
    |audio:not([controls]) {
    |  display: none;
    |  height: 0;
    |}
    |
    |abbr[title] {
    |  border-bottom: dotted 1px;
    |  cursor: help;
    |  text-decoration: none;
    |}
    |
    |code,
    |pre,
    |samp {
    |  white-space: pre-wrap;
    |}
    |
    |code {
    |  line-height: 1;
    |}
    |
    |dfn {
    |  font-style: italic;
    |}
    |
    |h1,
    |.h1 {
    |  display: block;
    |  font-size: 2em;
    |  line-height: 1.5;
    |  margin: 0.75em 0;
    |  font-weight: normal;
    |}
    |
    |h2,
    |.h2 {
    |  display: block;
    |  font-size: 1.5em;
    |  line-height: 1;
    |  margin: 0.5em 0;
    |  font-weight: normal;
    |}
    |
    |h3,
    |.h3 {
    |  display: block;
    |  font-size: 1.1875em;
    |  line-height: 1.2631578947;
    |  margin: 1.2631578947em 0;
    |  font-weight: normal;
    |}
    |
    |h4,
    |.h4 {
    |  display: block;
    |  font-size: 1em;
    |  line-height: 1.5;
    |  margin: 1.5em 0;
    |  font-weight: normal;
    |}
    |
    |h5,
    |.h5 {
    |  display: block;
    |  font-size: 0.8125em;
    |  line-height: 1.8461538462;
    |  margin: 1.8461538462em 0;
    |  font-weight: normal;
    |}
    |
    |h6,
    |.h6 {
    |  display: block;
    |  font-size: 0.75em;
    |  line-height: 2;
    |  margin: 2em 0;
    |  font-weight: normal;
    |}
    |
    |body {
    |  font-size: 100%;
    |  width: 100%;
    |  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
    |    Oxygen-Sans, Ubuntu, Cantarell, "Helvetica Neue", sans-serif;
    |  padding: 1rem;
    |  line-height: 1.5;
    |	overflow-wrap: break-word;
    |  color: var(--color-main-area, #262a33);
    |}
    |
    |q {
    |  quotes: none;
    |}
    |
    |q::after,
    |q::before {
    |  content: none;
    |}
    |
    |a:focus {
    |  outline: dotted thin;
    |}
    |
    |a:active,
    |a:hover {
    |  outline: 0;
    |}
    |
    |img {
    |  border: 0;
    |  max-width: 100%;
    |}
    |
    |table img {
    |  max-width: none;
    |}
    |
    |blockquote {
    |  padding: 0.2em 1.2em !important;
    |  margin: 0 !important;
    |  border: 3px solid #657ee4 !important;
    |  border-width: 0 0 0 3px !important;
    |}
    |
    |blockquote blockquote {
    |  font-size: 1em;
    |}
    |
    |blockquote blockquote blockquote {
    |  padding: 0 !important;
    |  border: none !important;
    |}
    |
    |[hidden] {
    |  display: none;
    |}
    |</style>
    |  <meta name="viewport" content="width=462, maximum-scale=2"> 
    |</head>Sent to myself
""".trimMargin("|")
// endregion
