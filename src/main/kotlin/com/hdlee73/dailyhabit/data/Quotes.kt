package com.hdlee73.dailyhabit.data

import java.time.LocalDate

data class Quote(val ko: String, val en: String, val author: String)

/** 일반용 '오늘의 명언'. 날짜마다 하나씩 돌아가며 보여준다. */
object Quotes {
    val all = listOf(
        Quote("천 리 길도 한 걸음부터 시작된다.", "A journey of a thousand miles begins with a single step.", "노자 (Lao Tzu)"),
        Quote("우리가 반복적으로 하는 행동이 곧 우리 자신이다. 그러므로 탁월함은 행동이 아니라 습관이다.", "We are what we repeatedly do. Excellence, then, is not an act, but a habit.", "윌 듀런트 (Will Durant)"),
        Quote("성공은 매일 반복한 작은 노력들의 합이다.", "Success is the sum of small efforts, repeated day in and day out.", "로버트 콜리어 (Robert Collier)"),
        Quote("시작하는 방법은 말을 멈추고 행동하는 것이다.", "The way to get started is to quit talking and begin doing.", "월트 디즈니 (Walt Disney)"),
        Quote("행복은 이미 만들어진 것이 아니다. 당신의 행동에서 나온다.", "Happiness is not something ready made. It comes from your own actions.", "달라이 라마 (Dalai Lama)"),
        Quote("인생은 자전거를 타는 것과 같다. 균형을 잡으려면 계속 움직여야 한다.", "Life is like riding a bicycle. To keep your balance, you must keep moving.", "알베르트 아인슈타인 (Albert Einstein)"),
        Quote("할 수 있다고 믿든 할 수 없다고 믿든, 당신이 옳다.", "Whether you think you can, or you think you can't — you're right.", "헨리 포드 (Henry Ford)"),
        Quote("오늘 할 수 있는 일을 내일로 미루지 마라.", "Never put off till tomorrow what you can do today.", "토머스 제퍼슨 (Thomas Jefferson)"),
        Quote("잃어버린 시간은 다시 찾을 수 없다.", "Lost time is never found again.", "벤저민 프랭클린 (Benjamin Franklin)"),
        Quote("일찍 자고 일찍 일어나면 건강하고 부유하고 지혜로워진다.", "Early to bed and early to rise makes a man healthy, wealthy, and wise.", "벤저민 프랭클린 (Benjamin Franklin)"),
        Quote("내일 죽을 것처럼 살고, 영원히 살 것처럼 배워라.", "Live as if you were to die tomorrow. Learn as if you were to live forever.", "마하트마 간디 (Mahatma Gandhi)"),
        Quote("세상에서 보고 싶은 변화가 있다면 당신 스스로 그 변화가 되어라.", "Be the change that you wish to see in the world.", "마하트마 간디 (Mahatma Gandhi)"),
        Quote("어둠은 어둠을 몰아낼 수 없다. 오직 빛만이 할 수 있다.", "Darkness cannot drive out darkness; only light can do that.", "마틴 루서 킹 주니어 (Martin Luther King Jr.)"),
        Quote("계단 전체를 볼 필요는 없다. 그저 첫 계단을 올라라.", "You don't have to see the whole staircase, just take the first step.", "마틴 루서 킹 주니어 (Martin Luther King Jr.)"),
        Quote("나는 실패한 것이 아니다. 잘 되지 않는 방법 만 가지를 찾았을 뿐이다.", "I have not failed. I've just found 10,000 ways that won't work.", "토머스 에디슨 (Thomas Edison)"),
        Quote("우리의 가장 큰 영광은 한 번도 넘어지지 않는 것이 아니라, 넘어질 때마다 일어나는 데 있다.", "Our greatest glory is not in never falling, but in rising every time we fall.", "올리버 골드스미스 (Oliver Goldsmith)"),
        Quote("아는 것만으로는 충분하지 않다. 적용해야 한다. 바라는 것만으로는 충분하지 않다. 행해야 한다.", "Knowing is not enough; we must apply. Willing is not enough; we must do.", "괴테 (Johann Wolfgang von Goethe)"),
        Quote("작은 일을 큰 사랑으로 하라.", "Do small things with great love.", "마더 데레사 (Mother Teresa)"),
        Quote("어제로부터 배우고, 오늘을 살며, 내일을 꿈꿔라.", "Learn from yesterday, live for today, hope for tomorrow.", "알베르트 아인슈타인 (Albert Einstein)"),
        Quote("단순함은 궁극의 정교함이다.", "Simplicity is the ultimate sophistication.", "레오나르도 다빈치 (Leonardo da Vinci)"),
        Quote("되고 싶었던 사람이 되기에 너무 늦은 때란 없다.", "It is never too late to be what you might have been.", "조지 엘리엇 (George Eliot)"),
        Quote("행동은 모든 성공의 기본 열쇠다.", "Action is the foundational key to all success.", "파블로 피카소 (Pablo Picasso)"),
        Quote("멈추지만 않는다면 얼마나 천천히 가는지는 문제가 되지 않는다.", "It does not matter how slowly you go as long as you do not stop.", "공자 (Confucius)"),
        Quote("아는 것을 안다고 하고, 모르는 것을 모른다고 하는 것, 이것이 아는 것이다.", "To know what you know and what you do not know, that is true knowledge.", "공자 (Confucius)"),
        Quote("삶이 있는 한 희망은 있다.", "While there's life, there's hope.", "키케로 (Cicero)"),
        Quote("감사하는 마음은 가장 위대한 미덕일 뿐 아니라 모든 미덕의 어버이다.", "Gratitude is not only the greatest of virtues, but the parent of all the others.", "키케로 (Cicero)"),
        Quote("행복한 삶을 위해 필요한 것은 아주 적다. 모든 것은 당신 안에, 당신의 생각하는 방식에 있다.", "Very little is needed to make a happy life; it is all within yourself, in your way of thinking.", "마르쿠스 아우렐리우스 (Marcus Aurelius)"),
        Quote("좋은 사람이 어떠해야 하는지 논쟁하느라 시간을 허비하지 말고, 좋은 사람이 되어라.", "Waste no more time arguing what a good man should be. Be one.", "마르쿠스 아우렐리우스 (Marcus Aurelius)"),
        Quote("운은 준비가 기회를 만났을 때 생긴다.", "Luck is what happens when preparation meets opportunity.", "세네카 (Seneca)"),
        Quote("우리는 현실보다 상상 속에서 더 자주 괴로워한다.", "We suffer more often in imagination than in reality.", "세네카 (Seneca)"),
        Quote("변화하지 않는 것은 변화한다는 사실뿐이다.", "The only constant in life is change.", "헤라클레이토스 (Heraclitus)"),
        Quote("검토하지 않는 삶은 살 가치가 없다.", "The unexamined life is not worth living.", "소크라테스 (Socrates)"),
        Quote("자신을 이기는 것이 가장 위대한 승리다.", "The first and best victory is to conquer self.", "플라톤 (Plato)"),
        Quote("시작이 반이다.", "Well begun is half done.", "아리스토텔레스 (Aristotle)"),
        Quote("인내는 쓰지만 그 열매는 달다.", "Patience is bitter, but its fruit is sweet.", "장 자크 루소 (Jean-Jacques Rousseau)"),
        Quote("가장 어두운 시간은 해 뜨기 직전이다.", "The darkest hour is just before the dawn.", "토머스 풀러 (Thomas Fuller)"),
        Quote("당신이 할 수 있는 일, 또는 꿈꾸는 일이 있다면 시작하라. 대담함 속에 천재성과 힘과 마법이 있다.", "Whatever you can do, or dream you can, begin it. Boldness has genius, power and magic in it.", "괴테 (Goethe)"),
        Quote("1년 후에는 오늘 시작했더라면 하고 바라게 될 것이다.", "A year from now you may wish you had started today.", "캐런 램 (Karen Lamb)"),
        Quote("동기는 시작하게 하고, 습관은 계속하게 한다.", "Motivation is what gets you started. Habit is what keeps you going.", "짐 론 (Jim Rohn)"),
        Quote("훈련은 목표와 성취를 잇는 다리다.", "Discipline is the bridge between goals and accomplishment.", "짐 론 (Jim Rohn)"),
        Quote("당신의 시간은 한정되어 있다. 다른 사람의 삶을 사느라 낭비하지 마라.", "Your time is limited, so don't waste it living someone else's life.", "스티브 잡스 (Steve Jobs)"),
        Quote("늘 갈망하고, 늘 우직하게.", "Stay hungry, stay foolish.", "스티브 잡스 (Steve Jobs)"),
        Quote("미래를 예측하는 가장 좋은 방법은 미래를 만드는 것이다.", "The best way to predict the future is to create it.", "피터 드러커 (Peter Drucker)"),
        Quote("측정할 수 없으면 관리할 수 없다.", "What gets measured gets managed.", "피터 드러커 (Peter Drucker)"),
        Quote("두려움 없는 사람이 용감한 것이 아니라, 두려움을 이겨내는 사람이 용감한 것이다.", "The brave man is not he who does not feel afraid, but he who conquers that fear.", "넬슨 만델라 (Nelson Mandela)"),
        Quote("해내기 전까지는 늘 불가능해 보인다.", "It always seems impossible until it's done.", "넬슨 만델라 (Nelson Mandela)"),
        Quote("사랑받고 싶다면 사랑하라, 그리고 사랑스러워져라.", "If you would be loved, love, and be lovable.", "벤저민 프랭클린 (Benjamin Franklin)"),
        Quote("친절한 말은 짧고 하기 쉽지만, 그 메아리는 끝이 없다.", "Kind words can be short and easy to speak, but their echoes are truly endless.", "마더 데레사 (Mother Teresa)"),
        Quote("사람들은 당신이 한 말과 행동은 잊어도, 당신이 그들에게 준 느낌은 결코 잊지 않는다.", "People will forget what you said and did, but people will never forget how you made them feel.", "마야 안젤루 (Maya Angelou)"),
        Quote("행복은 향수와 같아서, 남에게 뿌리면 자신에게도 몇 방울 묻는다.", "Happiness is a perfume you cannot pour on others without getting a few drops on yourself.", "랄프 왈도 에머슨 (Ralph Waldo Emerson)"),
        Quote("하루하루를 수확으로 판단하지 말고 뿌린 씨앗으로 판단하라.", "Don't judge each day by the harvest you reap but by the seeds that you plant.", "로버트 루이스 스티븐슨 (Robert Louis Stevenson)"),
        Quote("고요한 바다는 결코 숙련된 뱃사람을 만들지 못한다.", "A smooth sea never made a skilled sailor.", "영국 속담 (English proverb)"),
        Quote("웃음 없는 하루는 낭비한 하루다.", "A day without laughter is a day wasted.", "찰리 채플린 (Charlie Chaplin)"),
        Quote("인생은 가까이서 보면 비극이지만 멀리서 보면 희극이다.", "Life is a tragedy when seen in close-up, but a comedy in long-shot.", "찰리 채플린 (Charlie Chaplin)"),
        Quote("진정한 재산은 금과 은이 아니라 건강이다.", "It is health that is real wealth and not pieces of gold and silver.", "마하트마 간디 (Mahatma Gandhi)"),
        Quote("몸을 잘 돌보라. 그것이 당신이 살아야 할 유일한 장소다.", "Take care of your body. It's the only place you have to live.", "짐 론 (Jim Rohn)"),
        Quote("독서는 정신에, 운동은 몸에 같은 역할을 한다.", "Reading is to the mind what exercise is to the body.", "조지프 애디슨 (Joseph Addison)"),
        Quote("매일이 일 년 중 가장 좋은 날임을 마음에 새겨라.", "Write it on your heart that every day is the best day in the year.", "랄프 왈도 에머슨 (Ralph Waldo Emerson)"),
        Quote("작은 기회로부터 종종 위대한 업적이 시작된다.", "Small opportunities are often the beginning of great enterprises.", "데모스테네스 (Demosthenes)"),
        Quote("물방울이 바위를 뚫는 것은 힘이 아니라 꾸준함 때문이다.", "Constant dripping hollows out a stone.", "루크레티우스 (Lucretius)"),
    )

    fun forDate(date: LocalDate): Quote = all[(date.toEpochDay() % all.size).toInt().let { if (it < 0) it + all.size else it }]

    fun indexFor(date: LocalDate): Int = all.indexOf(forDate(date))
}
