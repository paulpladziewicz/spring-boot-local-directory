class TimerBlock {
    static get toolbox() {
        return {
            title: 'Countdown Timer',
            icon: '<i class="bi bi-clock"></i>' // Bootstrap icon example
        };
    }

    constructor({ data, api }) {
        this.api = api;
        this.data = {
            countdownDate: data.countdownDate || ''
        };
        this.wrapper = undefined;
    }

    render() {
        this.wrapper = document.createElement('div');
        this.wrapper.classList.add('editor-timer-block');

        const input = document.createElement('input');
        input.type = 'datetime-local';
        input.classList.add('form-control', 'mb-3');
        input.value = this.data.countdownDate ? new Date(this.data.countdownDate).toISOString().slice(0, -1) : '';

        input.addEventListener('input', (event) => {
            this.data.countdownDate = event.target.value;
            this.updatePreview();
        });

        this.wrapper.appendChild(input);
        this.previewElement = document.createElement('div');
        this.updatePreview();
        this.wrapper.appendChild(this.previewElement);

        return this.wrapper;
    }

    updatePreview() {
        this.previewElement.innerHTML = ''; // Clear previous preview
        if (this.data.countdownDate) {
            const date = new Date(this.data.countdownDate);
            if (isNaN(date.getTime())) {
                this.previewElement.innerHTML = '<p class="text-danger">Invalid date format</p>';
                return;
            }

            const countdownWrapper = document.createElement('div');
            countdownWrapper.classList.add('countdown', 'd-flex', 'mb-3');
            countdownWrapper.setAttribute('data-countdown-date', this.data.countdownDate);

            countdownWrapper.innerHTML = `
                <div class="text-center">
                    <div class="h4 mb-0" data-days>0</div>
                    <span class="fs-sm">days</span>
                </div>
                <span class="blinking fs-xl mx-2">:</span>
                <div class="text-center">
                    <div class="h4 mb-0" data-hours>0</div>
                    <span class="fs-sm">hours</span>
                </div>
                <span class="blinking fs-xl mx-2">:</span>
                <div class="text-center">
                    <div class="h4 mb-0" data-minutes>0</div>
                    <span class="fs-sm">mins</span>
                </div>
                <span class="blinking fs-xl mx-2">:</span>
                <div class="text-center">
                    <div class="h4 mb-0" data-seconds>0</div>
                    <span class="fs-sm">secs</span>
                </div>
            `;

            this.previewElement.appendChild(countdownWrapper);
            this.startCountdown(countdownWrapper, date);
        }
    }

    startCountdown(element, targetDate) {
        const updateTimer = () => {
            const now = new Date();
            const timeDifference = targetDate - now;

            if (timeDifference <= 0) {
                element.innerHTML = '<div class="alert alert-danger">Time is up!</div>';
                clearInterval(interval);
                return;
            }

            const days = Math.floor(timeDifference / (1000 * 60 * 60 * 24));
            const hours = Math.floor((timeDifference % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            const minutes = Math.floor((timeDifference % (1000 * 60 * 60)) / (1000 * 60));
            const seconds = Math.floor((timeDifference % (1000 * 60)) / 1000);

            element.querySelector('[data-days]').textContent = days;
            element.querySelector('[data-hours]').textContent = hours;
            element.querySelector('[data-minutes]').textContent = minutes;
            element.querySelector('[data-seconds]').textContent = seconds;
        };

        updateTimer();
        const interval = setInterval(updateTimer, 1000);
    }

    save(blockContent) {
        return {
            countdownDate: this.data.countdownDate
        };
    }

    static get sanitize() {
        return {
            countdownDate: false
        };
    }
}
