/**
 * Dòng chữ về mã do hệ thống cấp, thay cho ô nhập mã.
 * @param code `undefined`: bản ghi đang thêm. `null`: bản ghi chưa có mã, không hiện gì. Chuỗi: mã hiện tại.
 */
export function CodeNote({ code }: { code: string | null | undefined }) {
  if (code === null) return null
  return (
    <p className="text-sm text-muted-foreground">
      {code === undefined ? 'Mã được cấp khi lưu.' : `Mã ${code} · do hệ thống cấp, không sửa được`}
    </p>
  )
}
