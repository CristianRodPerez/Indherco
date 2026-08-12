import type { Movement } from '../api/types';

type MovementTotals = {
  production: number;
  dispatch: number;
  consumption: number;
  cancellations: number;
};

type PdfPage = {
  lines: string[];
};

const pageWidth = 842;
const pageHeight = 595;
const margin = 28;
const rowHeight = 14;
const columns = [
  { title: 'ID', x: 28, width: 24 },
  { title: 'Fecha', x: 54, width: 45 },
  { title: 'Hora', x: 102, width: 58 },
  { title: 'Usuario', x: 164, width: 68 },
  { title: 'Tipo', x: 236, width: 62 },
  { title: 'Entidad', x: 302, width: 54 },
  { title: 'Producto/Insumo', x: 360, width: 112 },
  { title: 'Cantidad', x: 476, width: 64 },
  { title: 'Stock ant.', x: 544, width: 52 },
  { title: 'Stock nuevo', x: 600, width: 62 },
  { title: 'Estado', x: 666, width: 58 },
  { title: 'Observacion', x: 728, width: 84 }
];

export function downloadMovementMonthlyPdf(month: string, movements: Movement[]) {
  const totals = calculateTotals(movements);
  const generatedAt = new Date().toLocaleString('es-CL', { dateStyle: 'short', timeStyle: 'short' });
  const pages: PdfPage[] = [];
  let currentPage = createPage(pages, month, generatedAt, true, totals, movements.length);
  let y = 418;

  drawTableHeader(currentPage, y);
  y -= rowHeight;

  if (movements.length === 0) {
    addText(currentPage, margin, y, 'No hay movimientos registrados para este mes.', 10);
  }

  movements.forEach((movement) => {
    if (y < 42) {
      currentPage = createPage(pages, month, generatedAt, false, totals, movements.length);
      y = 520;
      drawTableHeader(currentPage, y);
      y -= rowHeight;
    }

    drawMovementRow(currentPage, y, movement);
    y -= rowHeight;
  });

  const pdf = buildPdf(pages);
  const blob = new Blob([pdf], { type: 'application/pdf' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `historial-movimientos-${month}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function createPage(
  pages: PdfPage[],
  month: string,
  generatedAt: string,
  includeSummary: boolean,
  totals: MovementTotals,
  movementCount: number
) {
  const page: PdfPage = { lines: [] };
  pages.push(page);

  addFill(page, 0, pageHeight - 34, pageWidth, 34, '0.118 0.227 0.541');
  addText(page, margin, 574, 'Indherco - Historial mensual de movimientos', 16, '1 1 1');
  addText(page, margin, 542, `Mes: ${formatMonth(month)}`, 10);
  addText(page, margin, 527, `Generado: ${generatedAt}`, 10);
  addText(page, margin, 512, `Total movimientos: ${movementCount}`, 10);

  if (includeSummary) {
    addText(page, margin, 486, 'Resumen mensual', 12);
    addText(page, margin, 466, `Produccion: ${totals.production}`, 10);
    addText(page, 170, 466, `Despacho: ${totals.dispatch}`, 10);
    addText(page, 300, 466, `Consumo: ${totals.consumption}`, 10);
    addText(page, 430, 466, `Anulaciones: ${totals.cancellations}`, 10);
  }

  return page;
}

function drawTableHeader(page: PdfPage, y: number) {
  addFill(page, margin, y - 4, 786, 16, '0.145 0.388 0.922');
  columns.forEach((column) => addText(page, column.x, y + 1, column.title, 7, '1 1 1'));
}

function drawMovementRow(page: PdfPage, y: number, movement: Movement) {
  const cells = [
    String(movement.id),
    formatDate(movement.movementDate),
    formatTime(movement.registeredAt),
    movement.registeredBy,
    labelType(movement.movementType),
    movement.entityType === 'PRODUCTO' ? 'Producto' : 'Insumo',
    movement.itemName,
    `${movement.quantity} ${movement.unitOfMeasure}`,
    String(movement.previousStock),
    String(movement.newStock),
    labelStatus(movement.status),
    movement.observation || '-'
  ];

  addStroke(page, margin, y - 5, 786, 0);
  cells.forEach((cell, index) => {
    const column = columns[index];
    addText(page, column.x, y, truncate(cell, Math.floor(column.width / 4)), 7);
  });
}

function calculateTotals(movements: Movement[]): MovementTotals {
  return movements.reduce(
    (totals, movement) => {
      if (movement.movementType === 'PRODUCCION') totals.production += Number(movement.quantity);
      if (movement.movementType === 'DESPACHO') totals.dispatch += Number(movement.quantity);
      if (movement.movementType === 'CONSUMO') totals.consumption += Number(movement.quantity);
      if (movement.movementType === 'ANULACION') totals.cancellations += Number(movement.quantity);
      return totals;
    },
    { production: 0, dispatch: 0, consumption: 0, cancellations: 0 }
  );
}

function buildPdf(pages: PdfPage[]) {
  const objects: string[] = [];
  const pageObjectIds: number[] = [];

  objects.push('<< /Type /Catalog /Pages 2 0 R >>');
  objects.push('');
  objects.push('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>');

  pages.forEach((page, index) => {
    const pageObjectId = objects.length + 1;
    const contentObjectId = pageObjectId + 1;
    pageObjectIds.push(pageObjectId);
    addText(page, pageWidth - 88, 20, `Pagina ${index + 1} de ${pages.length}`, 8, '0.392 0.455 0.545');

    objects.push(
      `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${pageWidth} ${pageHeight}] /Resources << /Font << /F1 3 0 R >> >> /Contents ${contentObjectId} 0 R >>`
    );
    objects.push(streamFor(page.lines.join('\n')));
  });

  objects[1] = `<< /Type /Pages /Kids [${pageObjectIds.map((id) => `${id} 0 R`).join(' ')}] /Count ${pages.length} >>`;

  const chunks = ['%PDF-1.4\n'];
  const offsets = [0];
  objects.forEach((object, index) => {
    offsets.push(byteLength(chunks.join('')));
    chunks.push(`${index + 1} 0 obj\n${object}\nendobj\n`);
  });

  const xrefOffset = byteLength(chunks.join(''));
  chunks.push(`xref\n0 ${objects.length + 1}\n`);
  chunks.push('0000000000 65535 f \n');
  offsets.slice(1).forEach((offset) => chunks.push(`${String(offset).padStart(10, '0')} 00000 n \n`));
  chunks.push(`trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefOffset}\n%%EOF`);

  return chunks.join('');
}

function streamFor(content: string) {
  return `<< /Length ${byteLength(content)} >>\nstream\n${content}\nendstream`;
}

function addText(page: PdfPage, x: number, y: number, text: string, size: number, color = '0.059 0.090 0.165') {
  page.lines.push(`${color} rg BT /F1 ${size} Tf ${x} ${y} Td (${escapePdf(text)}) Tj ET`);
}

function addFill(page: PdfPage, x: number, y: number, width: number, height: number, color: string) {
  page.lines.push(`${color} rg ${x} ${y} ${width} ${height} re f`);
}

function addStroke(page: PdfPage, x: number, y: number, width: number, height: number) {
  page.lines.push(`0.886 0.910 0.941 RG ${x} ${y} ${width} ${height} re S`);
}

function byteLength(value: string) {
  return new TextEncoder().encode(value).length;
}

function escapePdf(value: string) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^\x20-\x7E]/g, '')
    .replace(/\\/g, '\\\\')
    .replace(/\(/g, '\\(')
    .replace(/\)/g, '\\)');
}

function truncate(value: string, maxLength: number) {
  if (value.length <= maxLength) return value;
  return `${value.slice(0, Math.max(0, maxLength - 3))}...`;
}

function labelType(type: Movement['movementType']) {
  if (type === 'PRODUCCION') return 'Produccion';
  if (type === 'DESPACHO') return 'Despacho';
  if (type === 'CONSUMO') return 'Consumo';
  return 'Anulacion';
}

function labelStatus(status: Movement['status']) {
  return status === 'ACTIVO' ? 'Activo' : 'Anulado';
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString('es-CL');
}

function formatTime(value: string) {
  return new Date(value).toLocaleTimeString('es-CL', {
    timeStyle: 'short'
  });
}

function formatMonth(value: string) {
  return new Date(`${value}-01T00:00:00`).toLocaleDateString('es-CL', {
    month: 'long',
    year: 'numeric'
  });
}
