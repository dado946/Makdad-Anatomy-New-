'use strict';

const fs = require('fs');
const path = require('path');

const DATA = '../open-anatomy-atlas/src/data';

const {
  readLists,
  conceptsFromLists,
  parseMeshes,
  elementKey,
  ATTRIBUTION
} = require(path.join(__dirname, DATA, 'bp3d'));

const { ZipArchive } = require(path.join(__dirname, DATA, 'zip'));
const { buildGraph, newConcept } = require(path.join(__dirname, DATA, 'graph'));
const { assemble } = require(path.join(__dirname, DATA, 'assemble'));
const { parseFMA2Obj, parseObj2FMA, inferParents } =
  require(path.join(__dirname, DATA, 'bp3d43'));

async function main() {
  const rawDir = path.resolve(process.argv[2]);
  const outDir = path.resolve(process.argv[3]);

  const metadata = path.join(rawDir, 'metadata');

  const manifest = parseFMA2Obj(
    fs.readFileSync(path.join(metadata, 'FMA2Obj.txt'), 'utf8')
  );

  const map = parseObj2FMA(
    fs.readFileSync(path.join(metadata, 'obj2FMA.html'), 'utf8')
  );

  const wanted = new Set(manifest.flatMap(row => row.fj));

  console.log('Reading anatomical structure lists...');

  const C = conceptsFromLists(readLists([rawDir]));

  for (const c of C.values()) c.direct.clear();

  for (const row of map) {
    if (row.fma) {
      newConcept(C, row.fma, row.name);
    }
  }

  for (const row of manifest) {
    const c = newConcept(C, row.fma);
    for (const fj of row.fj) c.direct.add(fj);
  }

  console.log('Reading downloaded bone source meshes...');

  const objs = new Map();
  const archives = [];

  const chunkDir = path.join(rawDir, 'chunks');

  const chunks = fs.readdirSync(chunkDir)
    .filter(name => name.endsWith('.zip'))
    .sort();

  for (const name of chunks) {
    const zip = new ZipArchive(path.join(chunkDir, name));
    archives.push(zip);

    for (const entry of zip.files()) {
      if (!/\.obj$/i.test(entry.name)) continue;

      const key = elementKey(entry.name);

      if (wanted.has(key) && !objs.has(key)) {
        objs.set(key, () => zip.read(entry).toString('latin1'));
      }
    }
  }

  if (!objs.size) {
    throw new Error('No anatomical meshes were found.');
  }

  console.log('Parsing meshes...');

  const meshes = await parseMeshes(objs, event => {
    if (event.message) console.log(event.message);
  });

  for (const zip of archives) zip.close();

  inferParents(C);

  console.log('Classifying anatomical structures...');

  const originalGraph = buildGraph(C, meshes);

  const bones = meshes.filter(
    (mesh, index) => originalGraph.cats[index] === 'bone'
  );

  if (!bones.length) {
    throw new Error('No bone structures were classified.');
  }

  console.log(`Found ${bones.length} bone meshes.`);

  console.log('Building the bone-only atlas...');

  const graph = buildGraph(C, bones);

  const counts = await assemble({
    meshes: bones,
    ...graph,
    outDir,
    mirror: { categories: [] },
    meta: {
      source: 'bp3d43-bones',
      name: 'MAKDAD ANATOMY - Skeleton',
      version: '4.3',
      attribution: ATTRIBUTION,
      license: 'CC BY 4.0',
      licenseUrl: 'https://creativecommons.org/licenses/by/4.0/',
      sourceUrl: 'https://lifesciencedb.jp/bp3d/',
      notes: [
        'Bone-only atlas generated from BodyParts3D 4.3.',
        'Original anatomical meshes are preserved.'
      ]
    }
  });

  console.log('Bone atlas created successfully.');
  console.log(JSON.stringify(counts, null, 2));
}

main().catch(error => {
  console.error('Bone atlas build failed:', error);
  process.exit(1);
});
