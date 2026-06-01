import argparse
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import LETTER
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import (
    HRFlowable,
    ListFlowable,
    ListItem,
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)


def clean_inline(text: str) -> str:
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("**", "")
        .replace("`", "")
    )


def styles():
    base = getSampleStyleSheet()
    base.add(
        ParagraphStyle(
            name="TitlePage",
            parent=base["Title"],
            fontName="Helvetica-Bold",
            fontSize=24,
            leading=30,
            alignment=TA_CENTER,
            textColor=colors.HexColor("#102033"),
            spaceAfter=18,
        )
    )
    base.add(
        ParagraphStyle(
            name="H1Custom",
            parent=base["Heading1"],
            fontName="Helvetica-Bold",
            fontSize=17,
            leading=22,
            textColor=colors.HexColor("#102033"),
            spaceBefore=16,
            spaceAfter=8,
        )
    )
    base.add(
        ParagraphStyle(
            name="H2Custom",
            parent=base["Heading2"],
            fontName="Helvetica-Bold",
            fontSize=12.5,
            leading=16,
            textColor=colors.HexColor("#1F4E79"),
            spaceBefore=10,
            spaceAfter=6,
        )
    )
    base.add(
        ParagraphStyle(
            name="BodyCustom",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=9.25,
            leading=13,
            textColor=colors.HexColor("#1F2933"),
            spaceAfter=5,
        )
    )
    base.add(
        ParagraphStyle(
            name="BulletCustom",
            parent=base["BodyCustom"],
            leftIndent=10,
            firstLineIndent=0,
            spaceAfter=2,
        )
    )
    return base


def build_table(lines, style_sheet):
    rows = []
    for line in lines:
        cells = [cell.strip() for cell in line.strip("|").split("|")]
        if all(set(cell) <= {"-", " "} for cell in cells):
            continue
        rows.append([Paragraph(clean_inline(cell), style_sheet["BodyCustom"]) for cell in cells])
    table = Table(rows, hAlign="LEFT", repeatRows=1)
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#EAF1F8")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.HexColor("#102033")),
                ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                ("GRID", (0, 0), (-1, -1), 0.25, colors.HexColor("#CBD5E1")),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 6),
                ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ]
        )
    )
    return table


def parse_markdown(markdown: str, style_sheet):
    story = []
    lines = markdown.splitlines()
    i = 0
    in_code = False
    current_list = []

    def flush_list():
        nonlocal current_list
        if current_list:
            story.append(
                ListFlowable(
                    [ListItem(Paragraph(clean_inline(item), style_sheet["BulletCustom"])) for item in current_list],
                    bulletType="bullet",
                    start="circle",
                    leftIndent=18,
                )
            )
            story.append(Spacer(1, 4))
            current_list = []

    while i < len(lines):
        line = lines[i].rstrip()
        if line.startswith("```"):
            flush_list()
            in_code = not in_code
            i += 1
            continue
        if in_code:
            i += 1
            continue
        if not line.strip():
            flush_list()
            story.append(Spacer(1, 4))
            i += 1
            continue
        if line.startswith("|"):
            flush_list()
            table_lines = []
            while i < len(lines) and lines[i].startswith("|"):
                table_lines.append(lines[i])
                i += 1
            story.append(build_table(table_lines, style_sheet))
            story.append(Spacer(1, 8))
            continue
        if line.startswith("# "):
            flush_list()
            if story:
                story.append(PageBreak())
            story.append(Paragraph(clean_inline(line[2:]), style_sheet["TitlePage"]))
            story.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor("#9DB4C8")))
            story.append(Spacer(1, 12))
        elif line.startswith("## "):
            flush_list()
            story.append(Paragraph(clean_inline(line[3:]), style_sheet["H1Custom"]))
        elif line.startswith("### "):
            flush_list()
            story.append(Paragraph(clean_inline(line[4:]), style_sheet["H2Custom"]))
        elif line.startswith("- "):
            current_list.append(line[2:])
        elif line[:3].isdigit() and line[3:5] == ". ":
            current_list.append(line[5:])
        else:
            flush_list()
            story.append(Paragraph(clean_inline(line), style_sheet["BodyCustom"]))
        i += 1
    flush_list()
    return story


def page_number(title):
    def add_page_number(canvas, doc):
        canvas.saveState()
        canvas.setFont("Helvetica", 8)
        canvas.setFillColor(colors.HexColor("#64748B"))
        canvas.drawRightString(7.5 * inch, 0.45 * inch, f"{title} | Page {doc.page}")
        canvas.restoreState()

    return add_page_number


def main():
    parser = argparse.ArgumentParser(description="Render a Markdown report to PDF.")
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--title", default="ACIP Report")
    args = parser.parse_args()

    style_sheet = styles()
    markdown = args.source.read_text(encoding="utf-8")
    story = parse_markdown(markdown, style_sheet)
    doc = SimpleDocTemplate(
        str(args.output),
        pagesize=LETTER,
        rightMargin=0.65 * inch,
        leftMargin=0.65 * inch,
        topMargin=0.6 * inch,
        bottomMargin=0.65 * inch,
        title=args.title,
        author="Codex",
    )
    doc.build(story, onFirstPage=page_number(args.title), onLaterPages=page_number(args.title))
    print(args.output)


if __name__ == "__main__":
    main()
