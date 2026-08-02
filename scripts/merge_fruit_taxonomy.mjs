import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "..");
const taxonomyPath = path.join(
  root,
  "data",
  "src",
  "main",
  "assets",
  "taxonomy",
  "candidate-taxonomy.json",
);
const releaseTaxonomyPath = path.join(
  root,
  "release-tool",
  "src",
  "main",
  "resources",
  "catalog",
  "candidate-taxonomy.json",
);
const snapshotPath =
  process.argv[2] ?? path.join(root, "tmp", "moa-n05-fruit-varieties.json");

const definitions = [
  fruit("coconut", "椰子", ["11", "12", "129"]),
  fruit("jujube", "棗子", ["22"]),
  fruit("custard-apple", "釋迦", ["31", "32"], ["鳳梨釋迦"]),
  fruit("ume", "梅", ["41"], ["青梅"]),
  fruit("chinese-bayberry", "楊梅", ["42"], ["中國楊梅"]),
  fruit("mulberry", "桑椹", ["43"], ["桑葚"]),
  fruit("strawberry", "草莓", ["45", "459"]),
  fruit("blueberry", "藍莓", ["46", "469"]),
  fruit("passion-fruit", "百香果", ["50", "51"]),
  fruit("sugarcane", "甘蔗", ["61"]),
  fruit("cherry-tomato", "小番茄", ["70", "72", "74"], ["聖女番茄", "玉女番茄"]),
  fruit("dragon-fruit", "紅龍果", ["811", "812", "813", "819"], ["火龍果"]),
  fruit("honey-jujube", "蜜棗", ["829"], ["台灣蜜棗"]),
  fruit("cherry", "櫻桃", ["839"]),
  fruit("durian", "榴槤", ["85", "859"]),
  fruit("mangosteen", "山竹", ["86", "869"]),
  fruit("rambutan", "紅毛丹", ["87"]),
  fruit("jabuticaba", "樹葡萄", ["88"], ["嘉寶果"]),
  fruit("other", "其他水果", ["91", "919"], ["其他"]),
  fruit("banana", "香蕉", ["A0", "A1", "A2", "A3", "A4"], ["芭蕉", "旦蕉"]),
  fruit("pineapple", "鳳梨", ["B0", "B1", "B2", "B6", "B7", "B8", "B9"], ["菠蘿"]),
  fruit("mandarin", "柑橘", ["C0", "C2", "C7", "C9"], ["茂谷柑"]),
  fruit("tankan", "桶柑", ["D1"]),
  fruit("orange", "甜橙", ["E1", "E9"], ["柳橙"]),
  fruit("mixed-citrus", "雜柑", ["F0", "F9"]),
  fruit("lemon", "檸檬", ["F1", "F5", "F6"], ["無子檸檬", "黃金檸檬"]),
  fruit("kumquat", "桔子", ["F4"], ["金桔"]),
  fruit("eggfruit", "蛋黃果", ["G1"], ["仙桃"]),
  fruit("abiu", "黃金果", ["G2"]),
  fruit("avocado", "酪梨", ["G3", "G39"], ["鱷梨"]),
  fruit("kiwi", "奇異果", ["G49"], ["獼猴桃"]),
  fruit("pepino", "香瓜梨", ["G5"], ["人參果"]),
  fruit("olive", "橄欖", ["G7"]),
  fruit("chestnut", "栗子", ["G8"]),
  fruit("jackfruit", "波蘿蜜", ["G9"], ["菠蘿蜜"]),
  fruit("cempedak", "榴槤蜜", ["G91"]),
  fruit("pomelo", "柚子", ["H1", "H6"], ["文旦", "西施柚"]),
  fruit("grapefruit", "葡萄柚", ["H4", "H41", "H49"]),
  fruit("papaya", "木瓜", ["I0", "I1", "I2", "I3", "I4"], ["青木瓜"]),
  fruit("lychee", "荔枝", ["J0", "J1", "J2", "J3", "J4", "J5"], ["玉荷包"]),
  fruit("longan", "龍眼", ["K3", "K4"], ["桂圓"]),
  fruit("loquat", "枇杷", ["L1", "L9"]),
  fruit("starfruit", "楊桃", ["M2", "M3"]),
  fruit("plum", "李", ["N0", "N2", "N3", "N4", "N5", "N6", "N9"], ["李子"]),
  fruit(
    "pear",
    "梨",
    ["O0", "O1", "O10", "O2", "O4", "O5", "O6", "O8", "O9", "O99", "OV", "OW"],
    ["水梨", "西洋梨"],
  ),
  fruit("guava", "番石榴", ["P0", "P1", "P2", "P3", "P4", "P5"], ["芭樂"]),
  fruit("wax-apple", "蓮霧", ["Q0", "Q1", "Q2", "Q3", "Q4", "Q5"]),
  fruit("mango", "芒果", ["R0", "R1", "R10", "R2", "R3", "R31", "R4", "R5", "R6", "R7", "R8"]),
  fruit("grape", "葡萄", ["S0", "S1", "S49", "S9"]),
  fruit("watermelon", "西瓜", ["T0", "T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9"]),
  fruit("oriental-melon", "甜瓜", ["V0", "V1", "V2"], ["美濃瓜"]),
  fruit("muskmelon", "洋香瓜", ["W0", "W1", "W2", "W4", "W5", "W6", "W7", "W8", "W9"], ["網紋洋香瓜"]),
  fruit("apple", "蘋果", ["X0", "X09", "X19", "X29", "X3", "X39", "X49", "X59", "X69"], ["富士蘋果"]),
  fruit("peach", "桃子", ["Y0", "Y1", "Y19", "Y2", "Y3", "Y39", "Y4", "Y5", "Y9"], ["水蜜桃", "甜桃"]),
  fruit("persimmon", "柿子", ["Z1", "Z4", "Z9"], ["甜柿"]),
];

const taxonomy = JSON.parse(fs.readFileSync(taxonomyPath, "utf8"));
const snapshot = JSON.parse(fs.readFileSync(snapshotPath, "utf8"));

if (snapshot.kindCode !== "N05") {
  throw new Error(`Expected an N05 snapshot, received ${snapshot.kindCode}`);
}

const variantsByCode = new Map(
  snapshot.varieties.map((variant) => [variant.commodityCode, variant]),
);
const assignedCodes = definitions.flatMap((definition) => definition.codes);
const duplicateCodes = assignedCodes.filter(
  (code, index) => assignedCodes.indexOf(code) !== index,
);
const missingCodes = [...variantsByCode.keys()].filter(
  (code) => !assignedCodes.includes(code),
);
const unknownCodes = assignedCodes.filter((code) => !variantsByCode.has(code));

if (duplicateCodes.length || missingCodes.length || unknownCodes.length) {
  throw new Error(
    JSON.stringify(
      {
        duplicateCodes: [...new Set(duplicateCodes)],
        missingCodes,
        unknownCodes,
      },
      null,
      2,
    ),
  );
}

const reviewDate = "2026-07-29";
const reviewer = "codex-image-review";
const fruitConcepts = definitions.map((definition) => {
  const variants = definition.codes.map((code) => variantsByCode.get(code));
  const officialMappings = variants.flatMap((variant) =>
    variant.markets.map((market) => ({
      commodityCode: variant.commodityCode,
      officialName: variant.officialName.trim(),
      market,
    })),
  );
  const aliases = unique([
    definition.householdName,
    ...definition.extraAliases,
    ...variants.map((variant) => variant.officialName.trim()),
    ...definition.codes,
  ]);

  return {
    stableId: `fruit.${definition.slug}`,
    householdName: definition.householdName,
    aliases,
    category: "FRUIT",
    publicationState: "PUBLISHED",
    officialMappings,
    image: {
      assetPath: `illustrations/catalog/fruit-${definition.slug}.webp`,
      generated: true,
      disclosure: "AI 生成示意圖，非實物照片。",
      reviewedAt: reviewDate,
      reviewedBy: reviewer,
      reviewStatus: "APPROVED",
    },
  };
});

const updated = {
  ...taxonomy,
  taxonomyVersion: "launch-all-produce-fruit-2026-07-29",
  artifactChecksum: "",
  review: {
    status: "APPROVED",
    reviewedAt: reviewDate,
    reviewedBy: reviewer,
    notes:
      "Expanded the approved vegetable catalog with exhaustive N05 fruit mappings and generated catalog illustrations.",
  },
  concepts: [
    ...fruitConcepts,
    ...taxonomy.concepts.filter((concept) => concept.category !== "FRUIT"),
  ],
  ambiguitySets: [
    ...taxonomy.ambiguitySets.filter(
      (ambiguity) => ambiguity.normalizedAlias !== "其他",
    ),
    {
      normalizedAlias: "其他",
      displayAlias: "其他",
      targetConceptIds: ["fruit.other", "vegetable.moa.ox1"],
    },
  ],
};
updated.artifactChecksum = checksum(updated);

const output = `${JSON.stringify(updated, null, 2)}\n`;
fs.writeFileSync(taxonomyPath, output, "utf8");
fs.writeFileSync(releaseTaxonomyPath, output, "utf8");

console.log(
  `Merged ${fruitConcepts.length} fruit concepts and ${snapshot.varieties.length} N05 variants; checksum ${updated.artifactChecksum}`,
);

function fruit(slug, householdName, codes, extraAliases = []) {
  return { slug, householdName, codes, extraAliases };
}

function unique(values) {
  return [...new Set(values.filter(Boolean))];
}

function compareNatural(left, right) {
  return left < right ? -1 : left > right ? 1 : 0;
}

function compareIgnoreCase(left, right) {
  return compareNatural(left.toUpperCase(), right.toUpperCase());
}

function canonical(artifact) {
  return {
    schemaVersion: artifact.schemaVersion,
    taxonomyVersion: artifact.taxonomyVersion,
    artifactChecksum: "",
    review: artifact.review,
    concepts: [...artifact.concepts]
      .sort((left, right) => compareNatural(left.stableId, right.stableId))
      .map((concept) => ({
        ...concept,
        aliases: [...concept.aliases].sort(compareIgnoreCase),
        officialMappings: [...concept.officialMappings].sort(
          (left, right) =>
            compareNatural(left.commodityCode, right.commodityCode) ||
            compareNatural(left.market, right.market) ||
            compareNatural(left.officialName, right.officialName),
        ),
      })),
    ambiguitySets: [...artifact.ambiguitySets]
      .sort((left, right) =>
        compareNatural(left.normalizedAlias, right.normalizedAlias),
      )
      .map((ambiguity) => ({
        ...ambiguity,
        targetConceptIds: [...ambiguity.targetConceptIds].sort(compareNatural),
      })),
  };
}

function checksum(artifact) {
  return crypto
    .createHash("sha256")
    .update(JSON.stringify(canonical(artifact)))
    .digest("hex");
}
