class ColumnsBlock {
    static get toolbox() {
        return {
            title: 'Columns',
            icon: '<i class="bi bi-layout-three-columns"></i>' // Example Bootstrap icon
        };
    }

    constructor({ data, api }) {
        this.api = api;
        this.data = {
            columns: data.columns || 2, // Default to 2 columns if not defined
            content: data.content || Array(2).fill([]) // Each column holds an array of blocks
        };
        this.wrapper = undefined;
        this.editors = [];
    }

    render() {
        this.wrapper = document.createElement('div');
        this.wrapper.classList.add('editor-columns-block');

        const columnsControl = document.createElement('select');
        columnsControl.classList.add('form-select', 'mb-3');
        columnsControl.innerHTML = `
            <option value="1" ${this.data.columns === 1 ? 'selected' : ''}>1 Column</option>
            <option value="2" ${this.data.columns === 2 ? 'selected' : ''}>2 Columns</option>
            <option value="3" ${this.data.columns === 3 ? 'selected' : ''}>3 Columns</option>
            <option value="4" ${this.data.columns === 4 ? 'selected' : ''}>4 Columns</option>
        `;

        columnsControl.addEventListener('change', (event) => {
            this.data.columns = parseInt(event.target.value);
            this.data.content = Array(this.data.columns).fill([]); // Reset content for each column
            this.updateColumns();
        });

        this.wrapper.appendChild(columnsControl);
        this.columnsContainer = document.createElement('div');
        this.wrapper.appendChild(this.columnsContainer);

        this.updateColumns();

        return this.wrapper;
    }

    updateColumns() {
        this.columnsContainer.innerHTML = '';
        this.columnsContainer.classList.add('row', 'g-3'); // Bootstrap row
        this.editors = []; // Reset editors

        for (let i = 0; i < this.data.columns; i++) {
            const colWrapper = document.createElement('div');
            colWrapper.classList.add('col');
            colWrapper.style.border = '1px dashed #ccc'; // Optional: for visual clarity

            const editorContainer = document.createElement('div');
            editorContainer.id = `editor-column-${i}`;

            colWrapper.appendChild(editorContainer);
            this.columnsContainer.appendChild(colWrapper);

            const editorInstance = new EditorJS({
                holder: editorContainer.id,
                tools: {
                    header: {
                        class: Header,
                        inlineToolbar: ['link'],
                        config: {
                            levels: [1, 2, 3],
                            defaultLevel: 2
                        }
                    },
                    paragraph: {
                        class: Paragraph,
                        inlineToolbar: true
                    },
                    image: {
                        class: ImageTool,
                        config: {
                            endpoints: {
                                byFile: '/uploadFile', // Backend endpoint for image uploads
                                byUrl: '/fetchUrl' // Backend endpoint for fetching image by URL
                            }
                        }
                    },
                    list: {
                        class: List,
                        inlineToolbar: true
                    }
                },
                data: {
                    time: new Date().getTime(),
                    blocks: this.data.content[i] || []
                }
            });

            this.editors.push(editorInstance);
        }
    }

    save(blockContent) {
        // Collect data from each editor instance
        return Promise.all(this.editors.map(editor => editor.save())).then(columnData => {
            return {
                columns: this.data.columns,
                content: columnData
            };
        });
    }

    static get sanitize() {
        return {
            columns: false,
            content: {
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
