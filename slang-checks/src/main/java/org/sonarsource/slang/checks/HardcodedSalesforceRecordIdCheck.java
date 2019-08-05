/*
 * SonarSource SLang
 * Copyright (C) 2018-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.slang.checks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.StringLiteralTree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;

@Rule(key = "S5379")
public class HardcodedSalesforceRecordIdCheck implements SlangCheck {

  public static final int SHORT_RECORD_ID_LENGTH = 15;
  public static final int LONG_RECORD_ID_LENGTH = 18;

  private static Pattern RECORD_ID_PATTERN = Pattern.compile("[A-Za-z0-9]+");

  /**
   * Uppercase list of Salesforce Record ID prefixes comes from
   * https://help.salesforce.com/articleView?id=000325244&type=1&mode=1 .
   * Prefixes are compared in a case-insensitive manner because:
   * https://help.salesforce.com/articleView?id=000324087&language=en_US&type=1&mode=1
   */
  private static final Set<String> RECORD_ID_PREFIXES = new HashSet<>(Arrays.asList(
    "0A2", "0AB", "0AD", "0AM", "0C0", "0C1", "0CA", "0CS", "0DR", "0EB", "0EH", "0EN", "0EO", "0EP", "0EQ", "0FR", "0GV", "0HC", "0HD", "0HT", "0IN", "0JA", "0JB", "0JD", "0JE",
    "0JF", "0JG", "0JI", "0JJ", "0JK", "0JL", "0JM", "0JN", "0JO", "0JP", "0JQ", "0JR", "0JS", "0JT", "0JU", "0JV", "0JW", "0JX", "0JY", "0JZ", "0K0", "0K2", "0K3", "0K4", "0K6",
    "0K7", "0K9", "0KA", "0KB", "0KC", "0KD", "0KE", "0KG", "0KH", "0KI", "0KM", "0KN", "0KO", "0KP", "0KQ", "0KR", "0KS", "0KT", "0KU", "0KY", "0KZ", "0L2", "0L3", "0L4", "0L5",
    "0LC", "0LD", "0LE", "0LF", "0LG", "0LH", "0LI", "0LJ", "0LM", "0LN", "0LO", "0LQ", "0LS", "0LU", "0LV", "0LW", "0LX", "0LY", "0M0", "0M1", "0M2", "0M3", "0M4", "0M5", "0M6",
    "0M9", "0MA", "0MB", "0MD", "0ME", "0MF", "0MG", "0MH", "0MI", "0MJ", "0MK", "0MN", "0MO", "0MP", "0MQ", "0MR", "0MS", "0MT", "0MU", "0MV", "0MW", "0MY", "0MZ", "0N0", "0N1",
    "0N2", "0N3", "0N4", "0N5", "0N9", "0NA", "0NB", "0NC", "0ND", "0NE", "0NF", "0NG", "0NH", "0NI", "0NJ", "0NK", "0NL", "0NM", "0NN", "0NO", "0NP", "0NQ", "0NR", "0NS", "0NT",
    "0NU", "0NV", "0NW", "0NX", "0NZ", "0O0", "0O1", "0O6", "0O7", "0O8", "0OA", "0OB", "0OC", "0OD", "0OE", "0OF", "0OG", "0OH", "0OI", "0OL", "0OM", "0OO", "0OP", "0OQ", "0OR",
    "0OV", "0OZ", "0P0", "0P1", "0P2", "0P9", "0PA", "0PB", "0PC", "0PD", "0PF", "0PK", "0PL", "0PM", "0PO", "0PP", "0PQ", "0PR", "0PS", "0PT", "0PU", "0PV", "0PX", "0PY", "0PZ",
    "0Q0", "0Q1", "0Q3", "0Q5", "0Q7", "0QB", "0QC", "0QD", "0QG", "0QI", "0QJ", "0QK", "0QL", "0QM", "0QN", "0QO", "0QP", "0QR", "0QT", "0QU", "0QV", "0QY", "0QZ", "0R0", "0R1",
    "0R2", "0R8", "0RA", "0RB", "0RC", "0RD", "0RE", "0RF", "0RG", "0RH", "0RI", "0RJ", "0RL", "0RM", "0RP", "0RR", "0RS", "0RT", "0RU", "0RV", "0RX", "0RY", "0RZ", "0S1", "0S2",
    "0SA", "0SE", "0SK", "0SL", "0SM", "0SN", "0SO", "0SP", "0SR", "0ST", "0SU", "0SV", "0SY", "0T0", "0T5", "0T6", "0TA", "0TE", "0TG", "0TH", "0TI", "0TJ", "0TN", "0TO", "0TR",
    "0TS", "0TT", "0TU", "0TV", "0TW", "0TY", "0U5", "0UA", "0UM", "0UP", "0UR", "0US", "0UT", "0W0", "0W1", "0W2", "0W3", "0W4", "0W5", "0W7", "0W8", "0WA", "0WB", "0WC", "0WD",
    "0WE", "0WF", "0WG", "0WH", "0WI", "0WJ", "0WK", "0WL", "0WM", "0WO", "0XA", "0XB", "0XC", "0XD", "0XE", "0XH", "0XR", "0XU", "0XV", "0YA", "0YM", "0YQ", "0YS", "0YU", "0YW",
    "0ZA", "0ZQ", "0ZX", "100", "101", "102", "10Y", "10Z", "110", "111", "112", "113", "11A", "130", "131", "149", "19I", "1AB", "1AR", "1BM", "1BR", "1CA", "1CB", "1CC", "1CF",
    "1CI", "1CL", "1CM", "1CP", "1CR", "1CS", "1DC", "1DE", "1DO", "1DP", "1DR", "1DS", "1ED", "1EF", "1EP", "1ES", "1EV", "1FS", "1GH", "1GP", "1HA", "1HB", "1HC", "1JS", "1L7",
    "1L8", "1LB", "1MA", "1MC", "1MP", "1MR", "1NR", "1O1", "1OZ", "1PM", "1PS", "1RP", "1RR", "1S1", "1SA", "1SL", "1SR", "1ST", "1TE", "1TS", "1VC", "1WK", "1WL", "200", "201",
    "202", "203", "204", "205", "208", "26Z", "2AS", "2CE", "2ED", "2EP", "2FE", "2FF", "2HF", "2LA", "2ON", "2SR", "300", "301", "307", "308", "309", "30A", "30C", "30D", "30E",
    "30F", "30G", "30L", "30M", "30P", "30Q", "30R", "30S", "30T", "30V", "30W", "30X", "310", "31A", "31C", "31D", "31I", "31O", "31S", "31V", "31W", "31X", "31Y", "31Z", "3DB",
    "3DP", "3DS", "3HP", "3J5", "3M0", "3M1", "3M2", "3M3", "3M4", "3M5", "3M6", "3MA", "3MB", "3MC", "3MD", "3ME", "3MF", "3MG", "3MH", "3MI", "3MJ", "3MK", "3ML", "3MM", "3MN",
    "3MO", "3MQ", "3MR", "3MS", "3MT", "3MU", "3MV", "3MW", "3N1", "3NA", "3NC", "3NO", "3NS", "3NT", "3NU", "3NV", "3NW", "3NX", "3NY", "3NZ", "3PB", "3PH", "3PP", "3PS", "3PX",
    "3SP", "3SS", "400", "401", "402", "403", "404", "405", "406", "407", "408", "410", "412", "413", "4A0", "4CI", "4CL", "4CO", "4DT", "4F0", "4F1", "4F2", "4F3", "4F4", "4F5",
    "4FE", "4FP", "4FT", "4IE", "4M5", "4M6", "4NA", "4NB", "4NC", "4ND", "4NW", "4PB", "4PV", "4SR", "4ST", "4SV", "4VE", "4WS", "4WT", "4WZ", "4XS", "500", "501", "550", "551",
    "552", "555", "557", "570", "571", "572", "573", "574", "5CS", "5PA", "5SP", "600", "601", "602", "604", "605", "606", "607", "608", "62C", "6AA", "6AB", "6AC", "6AD", "6EB",
    "6PS", "6SS", "700", "701", "707", "708", "709", "70A", "70B", "70C", "70D", "710", "711", "712", "713", "714", "715", "716", "729", "737", "750", "751", "752", "753", "754",
    "766", "777", "7DL", "7EH", "7EQ", "7ER", "7PV", "7TF", "7TG", "800", "801", "802", "803", "804", "805", "806", "807", "80D", "810", "811", "817", "820", "822", "823", "824",
    "825", "828", "829", "82B", "888", "889", "906", "907", "910", "911", "912", "9BV", "9DV", "9NV", "9YZ", "A00", "CF0", "E00", "KA0", "M00", "X00", "Z00"));

  @Override
  public void initialize(InitContext init) {
    init.register(StringLiteralTree.class, (ctx, tree) -> {
      if (isSalesforceRecordId(tree.content())) {
        ctx.reportIssue(tree, "Replace this hardcoded record ID");
      }
    });
  }

  private static boolean isSalesforceRecordId(String text) {
    return (text.length() == SHORT_RECORD_ID_LENGTH || text.length() == LONG_RECORD_ID_LENGTH) &&
      RECORD_ID_PREFIXES.contains(text.substring(0, 3).toUpperCase(Locale.ROOT)) &&
      RECORD_ID_PATTERN.matcher(text).matches();
  }

}
