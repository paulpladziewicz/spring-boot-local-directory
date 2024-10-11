class AlertBlock {
    static get toolbox() {
        return {
            title: 'Alert',
            icon: '<i class="bi bi-exclamation-triangle-fill"></i>' // Bootstrap icon example
        };
    }

    constructor({ data, api }) {
        this.api = api;
        this.data = {
            type: data.type || 'primary', // default alert type
            message: data.message || ''
        };
        
        this.wrapper = undefined;
    }

    render() {
        this.wrapper = document.createElement('div');
        this.wrapper.classList.add('editor-alert-block');
        
        const select = document.createElement('select');
        select.innerHTML = `
            <option value="primary" ${this.data.type === 'primary' ? 'selected' : ''}>Primary</option>
            <option value="secondary" ${this.data.type === 'secondary' ? 'selected' : ''}>Secondary</option>
            <option value="success" ${this.data.type === 'success' ? 'selected' : ''}>Success</option>
            <option value="danger" ${this.data.type === 'danger' ? 'selected' : ''}>Danger</option>
            <option value="warning" ${this.data.type === 'warning' ? 'selected' : ''}>Warning</option>
            <option value="info" ${this.data.type === 'info' ? 'selected' : ''}>Info</option>
            <option value="light" ${this.data.type === 'light' ? 'selected' : ''}>Light</option>
            <option value="dark" ${this.data.type === 'dark' ? 'selected' : ''}>Dark</option>
        `;
        select.classList.add('form-select', 'mb-2');
        
        const textarea = document.createElement('textarea');
        textarea.classList.add('form-control');
        textarea.placeholder = 'Enter alert message...';
        textarea.value = this.data.message;

        this.wrapper.appendChild(select);
        this.wrapper.appendChild(textarea);

        select.addEventListener('change', (event) => {
            this.data.type = event.target.value;
        });

        textarea.addEventListener('input', (event) => {
            this.data.message = event.target.value;
        });

        return this.wrapper;
    }

    save(blockContent) {
        const select = blockContent.querySelector('select');
        const textarea = blockContent.querySelector('textarea');
        return {
            type: select.value,
            message: textarea.value
        };
    }

    static get sanitize() {
        return {
            type: false,
            message: {
                b: true,
                a: {
                    href: true,
                },
                i: true,
                br: true,
                strong: true,
                em: true,
                p: true,
            }
        };
    }
}
