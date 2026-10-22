// SEO Analyzer - một ít JavaScript cho tương tác nhỏ.
// Trang vẫn dùng được đầy đủ nếu trình duyệt tắt JavaScript.

document.addEventListener("DOMContentLoaded", () => {

    // 1. Khi gửi form mất vài giây (phân tích, gọi AI): khoá nút và báo đang xử lý,
    //    tránh người dùng bấm nhiều lần.
    document.querySelectorAll("form[data-loading-form]").forEach((form) => {
        form.addEventListener("submit", () => {
            const button = form.querySelector("button[type=submit]");
            if (!button || button.disabled) return;
            const text = button.dataset.loadingText;
            // Đợi một nhịp để trình duyệt gửi form trước khi khoá nút
            setTimeout(() => {
                button.disabled = true;
                if (text) button.textContent = text;
            }, 0);
        });
    });

    // Khi quay lại trang bằng nút Back, trình duyệt có thể giữ nút đang bị khoá: mở lại.
    window.addEventListener("pageshow", () => {
        document.querySelectorAll("form[data-loading-form] button[type=submit]").forEach((button) => {
            button.disabled = false;
            if (button.dataset.originalText) button.textContent = button.dataset.originalText;
        });
    });
    document.querySelectorAll("form[data-loading-form] button[type=submit]").forEach((button) => {
        button.dataset.originalText = button.textContent;
    });

    // 2. Đếm ký tự cho bản viết lại title / meta description.
    //    Chuẩn hoá NFC và đếm theo ký tự thực, giống cách server đếm.
    document.querySelectorAll("[data-length-check]").forEach((field) => {
        const input = field.querySelector("textarea");
        const out = field.querySelector(".checker-out");
        const min = Number(field.dataset.min);
        const max = Number(field.dataset.max);
        if (!input || !out) return;

        const update = () => {
            const value = input.value.normalize("NFC").trim();
            const count = [...value].length;
            out.classList.remove("is-ok", "is-off");
            if (count === 0) {
                out.textContent = `0 ký tự, nên ${min}–${max}`;
            } else if (count < min) {
                out.textContent = `${count} ký tự: ngắn hơn mức nên có (${min}–${max}), thêm ${min - count} ký tự.`;
                out.classList.add("is-off");
            } else if (count > max) {
                out.textContent = `${count} ký tự: dài hơn mức nên có (${min}–${max}), bớt ${count - max} ký tự.`;
                out.classList.add("is-off");
            } else {
                out.textContent = `${count} ký tự: nằm trong mức nên có (${min}–${max}).`;
                out.classList.add("is-ok");
            }
        };
        input.addEventListener("input", update);
        update();
    });
});
