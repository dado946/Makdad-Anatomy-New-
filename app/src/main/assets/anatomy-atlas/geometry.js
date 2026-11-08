// Procedural primitive mesh generators: sphere, capsule/cylinder, box.
// Used to assemble a simplified labeled human figure (original geometry,
// no licensed/proprietary scan data).

function genSphere(radius, lat = 12, lon = 16) {
  const positions = [], normals = [], indices = [];

  for (let i = 0; i <= lat; i++) {
    const theta = (i * Math.PI) / lat;
    const st = Math.sin(theta), ct = Math.cos(theta);

    for (let j = 0; j <= lon; j++) {
      const phi = (j * 2 * Math.PI) / lon;
      const sp = Math.sin(phi), cp = Math.cos(phi);

      const x = cp * st;
      const y = ct;
      const z = sp * st;

      positions.push(x * radius, y * radius, z * radius);
      normals.push(x, y, z);
    }
  }

  // Outward-facing triangles
  for (let i = 0; i < lat; i++) {
    for (let j = 0; j < lon; j++) {
      const a = i * (lon + 1) + j;
      const b = a + lon + 1;

      indices.push(
        a, a + 1, b,
        b, a + 1, b + 1
      );
    }
  }

  return { positions, normals, indices };
}


// Capsule: cylinder body with hemispherical caps.
// Suitable for simplified bones and muscle shapes.

function genCapsule(radius, height, radial = 12) {
  const positions = [], normals = [], indices = [];

  const halfH = height / 2;
  const rings = 6;

  function pushRing(y, r, radialNormal, ny) {
    for (let j = 0; j <= radial; j++) {
      const phi = (j * 2 * Math.PI) / radial;

      const x = Math.cos(phi);
      const z = Math.sin(phi);

      positions.push(
        x * r,
        y,
        z * r
      );

      // Correct surface normal calculation
      const nx = x * radialNormal;
      const nz = z * radialNormal;

      const nl = Math.hypot(nx, ny, nz) || 1;

      normals.push(
        nx / nl,
        ny / nl,
        nz / nl
      );
    }
  }

  // Top hemisphere
  for (let i = 0; i <= rings; i++) {
    const t = (i / rings) * (Math.PI / 2);

    const y = halfH + Math.cos(t) * radius;
    const r = Math.sin(t) * radius;

    pushRing(
      y,
      r,
      Math.sin(t),
      Math.cos(t)
    );
  }

  // Bottom hemisphere
  for (let i = 0; i <= rings; i++) {
    const t = (i / rings) * (Math.PI / 2);

    const y = -halfH - Math.sin(t) * radius;
    const r = Math.cos(t) * radius;

    pushRing(
      y,
      r,
      Math.cos(t),
      -Math.sin(t)
    );
  }

  // Connect all rings with outward-facing triangles
  const ringCount = (rings + 1) * 2;

  for (let i = 0; i < ringCount - 1; i++) {
    for (let j = 0; j < radial; j++) {
      const a = i * (radial + 1) + j;
      const b = a + radial + 1;

      indices.push(
        a, a + 1, b,
        b, a + 1, b + 1
      );
    }
  }

  return { positions, normals, indices };
}


// Box generator

function genBox(sx, sy, sz) {
  const x = sx / 2;
  const y = sy / 2;
  const z = sz / 2;

  const positions = [
    // +x
    x,-y,-z, x,y,-z, x,y,z, x,-y,z,

    // -x
    -x,-y,z, -x,y,z, -x,y,-z, -x,-y,-z,

    // +y
    -x,y,-z, -x,y,z, x,y,z, x,y,-z,

    // -y
    -x,-y,z, -x,-y,-z, x,-y,-z, x,-y,z,

    // +z
    -x,-y,z, x,-y,z, x,y,z, -x,y,z,

    // -z
    x,-y,-z, -x,-y,-z, -x,y,-z, x,y,-z,
  ];

  const normals = [];

  const faceN = [
    [1,0,0],
    [-1,0,0],
    [0,1,0],
    [0,-1,0],
    [0,0,1],
    [0,0,-1]
  ];

  for (const n of faceN) {
    for (let i = 0; i < 4; i++) {
      normals.push(...n);
    }
  }

  const indices = [];

  for (let f = 0; f < 6; f++) {
    const o = f * 4;

    indices.push(
      o, o+1, o+2,
      o, o+2, o+3
    );
  }

  return { positions, normals, indices };
}


// Export

if (typeof module !== 'undefined') {
  module.exports = {
    genSphere,
    genCapsule,
    genBox
  };
}

if (typeof window !== 'undefined') {
  window.Geometry = {
    genSphere,
    genCapsule,
    genBox
  };
}
