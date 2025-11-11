//constantes de html via id
const video = document.getElementById('video');
const canvas = document.getElementById('canvas');
const resultEl = document.getElementById('result');
const startBtn = document.getElementById('startBtn');
//variables de control
let stream = null;
let animationId = null;
//funcion para mostrar el resultado y mandarlo
function showResult(id) {
    resultEl.textContent = id ?? '_';
    console.log(id);
    fetch('http://127.0.0.1:8080/api/id', {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({ id: 123455 })
    })
      .then(res => res.text())
    .then(data => console.log("Respuesta del servidor:", data))
    .catch(err => console.error("Error al enviar:", err));
}
//importacion de tryjsQR
async function  tryJsQR(imageData) {
    if (!window.jsQR) {
        await import('https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.min.js')
    .catch(e => {console.warn('No se cargo el jsQR', e); });
    console.log("qrjd cargado")
    }
    if (!window.jsQR) return null;
    const code = window.jsQR(imageData.data, imageData.width, imageData.height);
    return code ? code.data : null;
}
//funcion para el bucle de escaneo
async function  scanLoop() {
    if (video.videoWidth === 0 || video.videoHeight === 0) {
            animationId = requestAnimationFrame(scanLoop);
        return;
    }

canvas.width = video.videoWidth;
canvas.height = video.videoHeight;
const ctx = canvas.getContext('2d');
ctx.drawImage(video, 0,0, canvas.width, canvas.height);

const imageData = ctx.getImageData(0,0,canvas.width,canvas.height);
console.log("Píxeles analizados:", imageData.data.length);
const qr = await tryJsQR(imageData);
if (qr) {
    showResult(qr);
    cancelAnimationFrame(animationId);
    return;
}
animationId = requestAnimationFrame(scanLoop);
}
//funcion para prender la camara
async function startCamera() {
try {
    if(stream) {
        video.srcObject =stream;
    }
    else {
        const constraints = {
            video: {
                facingMode: {ideal: 'environment'},
                width: {ideal: 1280},
                height: {ideal: 720}
            },
            audio: false
        };
        stream = await navigator.mediaDevices.getUserMedia(constraints);
        video.srcObject = stream;
    
}
    video.play();
    showResult('buscando...');
    animationId = requestAnimationFrame(scanLoop);
}
catch(err) {
    console.error('No se pudo acceder a la camara', err);
    showResult('Error camara: '+ (err.message || err));
}
}
    startBtn.addEventListener('click', startCamera);
 window.addEventListener('beforeunload', () => {
    if (stream) {
        stream.getTracks().forEach(t => t.stop());
    }
 });   

    

    
